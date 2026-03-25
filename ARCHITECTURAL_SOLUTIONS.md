# 🔧 SentientGate — Architectural Solutions Guide

> Multiple solution approaches for every architectural design flaw, with trade-offs and implementation details.
> Date: 2026-03-25

---

## Flaw 1: Redundant DB Fetches for the Same User

**Problem**: 50 failed requests → 50 identical gRPC→PostgreSQL queries for the same user's history.

### Solution A — Local In-Memory Cache (Caffeine) ⭐ Recommended

Cache history in MCPService for 30 seconds using Caffeine:

```java
@Service
public class EventHistoryService {

    private final Cache<String, List<UserLogEvent>> historyCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(30))
        .maximumSize(10_000)
        .build();

    public List<UserLogEvent> getAllEventsInDuration(String uuid, int duration) {
        return historyCache.get(uuid, key -> fetchFromGrpc(key, duration));
    }

    private List<UserLogEvent> fetchFromGrpc(String uuid, int duration) {
        // existing gRPC call
    }
}
```

| Pros | Cons |
|------|------|
| Zero network calls for repeated UUIDs within 30s | Slightly stale data (max 30s old) |
| Simple to implement (1 dependency) | Memory usage grows with unique users |
| Sub-microsecond cache hits | Cache invalidation not event-driven |

**Dependency**: `implementation 'com.github.ben-manes.caffeine:caffeine:3.1.8'`

---

### Solution B — Redis-Based Distributed Cache

Cache history in Redis (shared across MCPService instances):

```java
public List<UserLogEvent> getAllEventsInDuration(String uuid, int duration) {
    String cacheKey = "history:" + uuid;
    String cached = redisTemplate.opsForValue().get(cacheKey).block();

    if (cached != null) {
        return objectMapper.readValue(cached, new TypeReference<>() {});
    }

    List<UserLogEvent> history = fetchFromGrpc(uuid, duration);
    redisTemplate.opsForValue().set(cacheKey,
        objectMapper.writeValueAsString(history),
        Duration.ofSeconds(30)).subscribe();
    return history;
}
```

| Pros | Cons |
|------|------|
| Shared across multiple MCPService instances | Extra Redis round-trip (~1ms) |
| Survives service restarts | Serialization/deserialization overhead |
| Consistent across replicas | Adds Redis dependency to MCPService |

---

### Solution C — MCPService Co-Consumes `user-logs` Topic (Event Sourcing)

MCPService subscribes to `user-logs` and maintains a **local sliding window** of events per UUID — eliminating gRPC calls entirely:

```java
@Service
public class LocalEventWindowService {

    // UUID → sliding window of last 10 min of events
    private final ConcurrentHashMap<String, Deque<LogEvent>> eventWindows = new ConcurrentHashMap<>();

    @KafkaListener(topics = "user-logs", groupId = "mcp-event-window")
    public void onLogEvent(LogEvent event) {
        eventWindows.computeIfAbsent(event.getUuid(), k -> new ConcurrentLinkedDeque<>())
            .addLast(event);
        evictOldEvents(event.getUuid());
    }

    public List<LogEvent> getHistory(String uuid) {
        Deque<LogEvent> window = eventWindows.getOrDefault(uuid, new ConcurrentLinkedDeque<>());
        return new ArrayList<>(window);
    }

    private void evictOldEvents(String uuid) {
        long cutoff = System.currentTimeMillis() - Duration.ofMinutes(10).toMillis();
        Deque<LogEvent> window = eventWindows.get(uuid);
        while (!window.isEmpty() && window.peekFirst().getTimestamp() < cutoff) {
            window.pollFirst();
        }
    }
}
```

| Pros | Cons |
|------|------|
| **Zero gRPC calls** — history is always local | Higher memory usage |
| Always up-to-date (event-driven) | Requires careful eviction logic |
| Eliminates race condition (Flaw 6) | Need separate Kafka consumer group |
| No dependency on LoggingService availability | History lost on restart (unless using compacted topic) |

---

### Solution D — Batch Dedup at Kafka Consumer Level

Group incoming events by UUID before processing:

```java
@KafkaListener(topics = "security-events", groupId = "mcp-analysis-group",
    containerFactory = "batchKafkaListenerContainerFactory")
public void onSecurityAlerts(List<SecurityAlertEvent> alerts) {
    // Group by UUID → process each UUID only once
    Map<String, List<SecurityAlertEvent>> byUuid = alerts.stream()
        .collect(Collectors.groupingBy(SecurityAlertEvent::getUuid));

    byUuid.forEach((uuid, events) -> {
        // Take the most severe alert
        SecurityAlertEvent worst = events.stream()
            .max(Comparator.comparing(e -> e.getErrorCode()))
            .orElse(events.get(0));
        mcpAnalysisService.analyze(worst);
    });
}
```

| Pros | Cons |
|------|------|
| One analysis per UUID per batch | Still fetches from DB per unique UUID |
| Works with existing architecture | Batch size affects dedup window |
| No new dependencies | Doesn't help with cross-batch redundancy |

---

## Flaw 2: Already-Blocked Users Still Get Fully Analyzed

**Problem**: After blocking UUID_A, remaining 49 events still trigger full analysis.

### Solution A — Redis Pre-Check ⭐ Recommended

```java
public void analyze(SecurityAlertEvent alert) {
    Boolean alreadyBlocked = redisTemplate.hasKey("blacklist:" + alert.getUuid()).block();
    if (Boolean.TRUE.equals(alreadyBlocked)) {
        log.debug("⏭️ Skipping already-blocked UUID: {}", alert.getUuid());
        return;
    }
    // ... proceed with analysis
}
```

| Pros | Cons |
|------|------|
| Single Redis `EXISTS` call (~0.1ms) | One extra Redis call per event |
| Eliminates all redundant work | Tiny race window before first block completes |

---

### Solution B — In-Memory Blocked Set

```java
private final Set<String> recentlyBlocked = ConcurrentHashMap.newKeySet();

public void analyze(SecurityAlertEvent alert) {
    if (recentlyBlocked.contains(alert.getUuid())) {
        return;
    }
    // ... analysis ...
    if (shouldBlock) {
        recentlyBlocked.add(alert.getUuid());
        enforcementService.blockUser(alert.getUuid(), strategy);
        // Auto-remove after TTL
        Executors.newSingleThreadScheduledExecutor()
            .schedule(() -> recentlyBlocked.remove(alert.getUuid()),
                strategy.getBlockDuration().toMinutes(), TimeUnit.MINUTES);
    }
}
```

| Pros | Cons |
|------|------|
| Zero network overhead | Not shared across MCPService instances |
| Fastest possible check | Memory leak risk if eviction fails |

---

### Solution C — Kafka Consumer Seek (Skip Events for Blocked UUIDs)

Use a custom `ConsumerInterceptor` to filter out events for already-blocked UUIDs before they reach the listener:

```java
public class BlockedUuidInterceptor implements ConsumerInterceptor<String, SecurityAlertEvent> {
    @Override
    public ConsumerRecords<String, SecurityAlertEvent> onConsume(ConsumerRecords<String, SecurityAlertEvent> records) {
        // Filter out records where key (UUID) is already blocked
        return filterBlockedRecords(records);
    }
}
```

| Pros | Cons |
|------|------|
| Events never reach the listener | More complex to implement |
| Reduces Kafka consumer load | Interceptor has limited access to Spring beans |

---

## Flaw 3: Synchronous Blocking LLM Call Inside Kafka Consumer Thread

**Problem**: AI inference blocks Kafka consumer threads for 5+ seconds.

### Solution A — Async Thread Pool ⭐ Recommended

Offload AI analysis to a dedicated thread pool:

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean("aiAnalysisExecutor")
    public Executor aiAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("ai-analysis-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}

@Service
public class McpAnalysisService {
    @Async("aiAnalysisExecutor")
    public void runAiAnalysis(SecurityAlertEvent alert, List<LogEvent> history) {
        AnomalyDetectionResponse response = aiClient.analyze(buildRequest(alert, history));
        if (response.isAnomaly() && response.getConfidenceScore() > 0.85) {
            enforcementService.blockUser(alert.getUuid(), aiAnomalyStrategy);
        }
    }
}
```

| Pros | Cons |
|------|------|
| Kafka consumer immediately freed | Need to manage thread pool sizing |
| AI can take as long as needed | Fire-and-forget — need error handling |
| Simple Spring annotation | Thread pool exhaustion under heavy load |

---

### Solution B — Separate Kafka Topic for AI Analysis

Publish events needing AI analysis to a dedicated `ai-analysis-requests` topic:

```java
// In McpAnalysisService — after rule-based strategies find nothing
if (noRuleBasedMatch && history.size() >= 5) {
    kafkaTemplate.send("ai-analysis-requests", alert.getUuid(), alert);
}

// Separate consumer with its own thread pool
@KafkaListener(topics = "ai-analysis-requests", groupId = "ai-analysis-group",
    concurrency = "10")
public void onAiAnalysisRequest(SecurityAlertEvent alert) {
    // This can block for 5s without affecting the main security pipeline
    aiAnomalyStrategy.analyze(alert);
}
```

| Pros | Cons |
|------|------|
| Complete isolation from main pipeline | Extra Kafka topic overhead |
| Independent scaling | Additional consumer group management |
| Can have higher concurrency | Adds latency to AI-based blocking |

---

### Solution C — WebClient Non-Blocking Call (Replace Feign)

Replace synchronous Feign client with reactive WebClient:

```java
@Service
public class ReactiveAIClient {
    private final WebClient webClient;

    public Mono<AnomalyDetectionResponse> analyze(AnomalyDetectionRequest request) {
        return webClient.post()
            .uri("/api/v1/analyze")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(AnomalyDetectionResponse.class)
            .timeout(Duration.ofSeconds(5))
            .onErrorReturn(new AnomalyDetectionResponse(false, 0.0, "error", 0));
    }
}
```

| Pros | Cons |
|------|------|
| Non-blocking — no thread starvation | Kafka consumer is still blocking (Spring Kafka) |
| Better resource utilization | Requires refactoring strategy interface to reactive |
| Built-in timeout and fallback | More complex error handling |

---

### Solution D — Two-Phase Strategy Pipeline

Split strategies into fast (sync) and slow (async):

```java
public void analyze(SecurityAlertEvent alert) {
    List<LogEvent> history = fetchHistory(alert);

    // Phase 1: Fast strategies (run synchronously on Kafka thread)
    Optional<ThreatStrategy> fastMatch = fastStrategies.stream()
        .filter(s -> s.isAvailable(alert, history))
        .findFirst();

    if (fastMatch.isPresent()) {
        enforcementService.blockUser(alert.getUuid(), fastMatch.get());
        return;
    }

    // Phase 2: Slow strategies (run asynchronously)
    CompletableFuture.runAsync(() -> {
        slowStrategies.stream()
            .filter(s -> s.isAvailable(alert, history))
            .findFirst()
            .ifPresent(s -> enforcementService.blockUser(alert.getUuid(), s));
    }, aiAnalysisExecutor);
}
```

| Pros | Cons |
|------|------|
| Best of both worlds | More complex code |
| Fast blocking for known patterns | Async strategies have delayed blocking |
| AI only runs when needed | Need to categorize strategies |

---

## Flaw 4: No Event Deduplication

**Problem**: 20 identical 404 events → 20 independent analyses.

### Solution A — ConcurrentHashMap Dedup Window ⭐ Recommended

```java
private final ConcurrentHashMap<String, Long> recentlyProcessed = new ConcurrentHashMap<>();

// Scheduled cleanup every 60 seconds
@Scheduled(fixedRate = 60_000)
public void cleanupDedupMap() {
    long cutoff = System.currentTimeMillis() - 60_000;
    recentlyProcessed.entrySet().removeIf(e -> e.getValue() < cutoff);
}

public void analyze(SecurityAlertEvent alert) {
    String dedupKey = alert.getUuid() + ":" + alert.getErrorCode();
    Long lastProcessed = recentlyProcessed.putIfAbsent(dedupKey, System.currentTimeMillis());

    if (lastProcessed != null && (System.currentTimeMillis() - lastProcessed) < 30_000) {
        return; // Duplicate within 30s window
    }
    recentlyProcessed.put(dedupKey, System.currentTimeMillis());
    // ... proceed
}
```

---

### Solution B — Redis-Based Distributed Dedup

```java
public void analyze(SecurityAlertEvent alert) {
    String dedupKey = "dedup:" + alert.getUuid() + ":" + alert.getErrorCode();

    Boolean isNew = redisTemplate.opsForValue()
        .setIfAbsent(dedupKey, "1", Duration.ofSeconds(30)).block();

    if (Boolean.FALSE.equals(isNew)) {
        return; // Already processed within window
    }
    // ... proceed
}
```

| Pros | Cons |
|------|------|
| Works across MCPService instances | Redis round-trip per event |
| Automatic TTL cleanup | Adds Redis dependency |

---

### Solution C — Kafka Streams Dedup with Window

Use Kafka Streams to deduplicate events in a time window before they reach the consumer:

```java
@Bean
public KStream<String, SecurityAlertEvent> dedupStream(StreamsBuilder builder) {
    return builder.stream("security-events")
        .groupByKey()
        .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(30)))
        .reduce((oldVal, newVal) -> newVal)  // Keep latest
        .toStream()
        .selectKey((windowedKey, value) -> windowedKey.key())
        .to("security-events-deduped");
}
```

| Pros | Cons |
|------|------|
| Dedup happens before consumer | Kafka Streams adds complexity |
| Declarative, built-in windowing | Extra topic + processing |
| Scalable | Higher infra cost |

---

## Flaw 5: Kafka Consumer Bottleneck (Only 3 Threads)

**Problem**: 3 threads cannot handle 1000+ events/second.

### Solution A — Increase Concurrency + Partitions ⭐ Recommended

```yaml
# Increase Kafka topic partitions
# Create topic with more partitions:
# kafka-topics --alter --topic security-events --partitions 12

# MCPService application.yml
listener:
  concurrency: 12  # Match partition count
```

---

### Solution B — True Batch Processing

Change from single-event to batch processing:

```java
@KafkaListener(topics = "security-events", groupId = "mcp-analysis-group",
    containerFactory = "batchListenerFactory")
public void onSecurityAlerts(List<SecurityAlertEvent> alerts) {
    // Group by UUID → one analysis per user
    alerts.stream()
        .collect(Collectors.groupingBy(SecurityAlertEvent::getUuid))
        .forEach((uuid, events) -> {
            SecurityAlertEvent representative = selectMostSevere(events);
            mcpAnalysisService.analyze(representative);
        });
}
```

---

### Solution C — Multiple Consumer Groups with Routing

Route different event types to different consumer groups:

```java
// High-priority: pattern matches, injection attempts
@KafkaListener(topics = "security-events", groupId = "mcp-critical",
    containerFactory = "highPriorityFactory",
    filter = "criticalEventFilter")
public void onCriticalAlert(SecurityAlertEvent alert) { ... }

// Normal priority: rate limits, generic errors
@KafkaListener(topics = "security-events", groupId = "mcp-normal",
    containerFactory = "normalFactory",
    filter = "normalEventFilter")
public void onNormalAlert(SecurityAlertEvent alert) { ... }
```

---

## Flaw 6: Race Condition Between Kafka Pipelines

**Problem**: MCPService queries history that may not be persisted yet.

### Solution A — MCPService Co-Consumes `user-logs` ⭐ Recommended

(Same as Flaw 1, Solution C — build local event window)

---

### Solution B — Configurable Delay Before Analysis

```java
@KafkaListener(topics = "security-events", groupId = "mcp-analysis-group")
public void onSecurityAlert(SecurityAlertEvent alert) {
    // Small delay to let LoggingService persist the events
    Thread.sleep(500);  // 500ms buffer
    mcpAnalysisService.analyze(alert);
}
```

| Pros | Cons |
|------|------|
| Dead simple | Wastes thread time |
| Usually sufficient | Not deterministic |

---

### Solution C — Event Ordering via Single Topic

Combine log events and security alerts into a **single topic** with discriminator:

```java
// Gateway publishes both to same topic
kafkaTemplate.send("gateway-events", uuid, GatewayEvent.log(logEvent));
kafkaTemplate.send("gateway-events", uuid, GatewayEvent.alert(alertEvent));

// MCPService consumes in order (same partition due to same key)
@KafkaListener(topics = "gateway-events")
public void onGatewayEvent(GatewayEvent event) {
    if (event.isLog()) {
        localEventWindow.add(event.getLogEvent());
    } else if (event.isAlert()) {
        // History is guaranteed to include preceding log
        mcpAnalysisService.analyze(event.getAlertEvent(), localEventWindow.getHistory(uuid));
    }
}
```

| Pros | Cons |
|------|------|
| Guaranteed ordering | Single topic for different event types |
| No race condition possible | MCPService now processes log events too |
| No gRPC needed | Higher coupling |

---

## Flaw 7: Dashboard Queries Hit PostgreSQL Directly

**Problem**: Heavy aggregate queries on every dashboard refresh.

### Solution A — Redis Caching Layer ⭐ Recommended

```java
@Service
public class CachedDashboardStatsService {
    private final RedisTemplate<String, String> redisTemplate;
    private final DashboardStatsService delegate;

    public DashboardSummaryStats getSummary(Instant start, Instant end) {
        String cacheKey = "dashboard:summary:" + start + ":" + end;
        String cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            return objectMapper.readValue(cached, DashboardSummaryStats.class);
        }

        DashboardSummaryStats stats = delegate.getSummary(start, end);
        redisTemplate.opsForValue().set(cacheKey,
            objectMapper.writeValueAsString(stats), Duration.ofSeconds(15));
        return stats;
    }
}
```

---

### Solution B — Materialized Views in PostgreSQL

```sql
CREATE MATERIALIZED VIEW dashboard_summary_1min AS
SELECT
    date_trunc('minute', occurred_at) AS bucket,
    COUNT(*) AS total_requests,
    SUM(CASE WHEN status_code >= 400 THEN 1 ELSE 0 END) AS errors,
    AVG(latency_ms) AS avg_latency,
    COUNT(DISTINCT client_ip) AS unique_ips
FROM gateway_logs
GROUP BY bucket;

-- Refresh every 30 seconds via scheduled job
REFRESH MATERIALIZED VIEW CONCURRENTLY dashboard_summary_1min;
```

---

### Solution C — Pre-Computed Aggregates via Kafka Streams

Use Kafka Streams to compute real-time aggregates:

```java
KTable<Windowed<String>, Long> requestCounts = builder
    .stream("user-logs")
    .groupBy((k, v) -> "global")
    .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
    .count();
```

---

### Solution D — Add Database Indexes

At minimum, add indexes for the most common query patterns:

```sql
CREATE INDEX idx_logs_occurred_at ON gateway_logs(occurred_at);
CREATE INDEX idx_logs_visitor_occurred ON gateway_logs(visitor_id, occurred_at);
CREATE INDEX idx_logs_ip_occurred ON gateway_logs(client_ip, occurred_at);
CREATE INDEX idx_logs_decision ON gateway_logs(decision);
CREATE INDEX idx_logs_status_occurred ON gateway_logs(status_code, occurred_at);
```

---

## Flaw 8: gRPC Server Not Implemented in LoggingService

**Problem**: No gRPC server exists → all history-dependent strategies are non-functional.

### Solution A — Implement gRPC Server ⭐ Recommended

**Step 1**: Add proto file to LoggingService (copy from MCPService):

```bash
cp MCPService/src/main/proto/user_log_event.proto LogingService/src/main/proto/
```

**Step 2**: Add gRPC dependencies to `build.gradle`:

```gradle
implementation 'net.devh:grpc-server-spring-boot-starter:3.0.0.RELEASE'
implementation 'io.grpc:grpc-protobuf'
implementation 'io.grpc:grpc-stub'
```

**Step 3**: Implement the service:

```java
@GrpcService
public class UserLogEventGrpcService extends UserLogEventServiceGrpc.UserLogEventServiceImplBase {

    private final GatewayLogRepository repository;

    @Override
    public void getUserEvents(UserLogEventsRequest request,
                              StreamObserver<UserLogEventResponse> responseObserver) {
        Instant since = Instant.now().minus(Duration.ofMinutes(request.getDuration()));
        List<GatewayLogEntity> logs = repository.findByVisitorIdAndOccurredAtAfter(
            request.getUuid(), since);

        List<UserLogEvent> protoEvents = logs.stream()
            .map(this::toProto)
            .toList();

        responseObserver.onNext(UserLogEventResponse.newBuilder()
            .addAllUserLogEvents(protoEvents)
            .build());
        responseObserver.onCompleted();
    }

    private UserLogEvent toProto(GatewayLogEntity entity) {
        return UserLogEvent.newBuilder()
            .setUuid(entity.getVisitorId())
            .setPath(entity.getPath())
            .setMethod(entity.getMethod())
            .setLatencyMs(entity.getLatencyMs())
            .setQueryParams(entity.getQueryParams() != null ? entity.getQueryParams() : "")
            .setClientIp(entity.getClientIp())
            .setStatusCode(entity.getStatusCode())
            .setRequestSize(entity.getRequestSize())
            .setTimestamp(entity.getOccurredAt().toEpochMilli())
            .setUserAgent(entity.getUserAgent() != null ? entity.getUserAgent() : "")
            .build();
    }
}
```

**Step 4**: Add repository method:

```java
// GatewayLogRepository.java
List<GatewayLogEntity> findByVisitorIdAndOccurredAtAfter(String visitorId, Instant since);
```

---

### Solution B — Replace gRPC with REST (Simpler)

If gRPC complexity is not needed, use a REST endpoint instead:

```java
// LoggingService — new controller
@GetMapping("/api/logs/history/{uuid}")
public List<GatewayLogEntity> getHistory(
    @PathVariable String uuid,
    @RequestParam int durationMinutes) {
    Instant since = Instant.now().minus(Duration.ofMinutes(durationMinutes));
    return repository.findByVisitorIdAndOccurredAtAfter(uuid, since);
}

// MCPService — use Feign instead of gRPC
@FeignClient(name = "LOGGING-SERVICE")
public interface LoggingServiceClient {
    @GetMapping("/logging-service/api/logs/history/{uuid}")
    List<LogEvent> getHistory(@PathVariable String uuid,
                              @RequestParam int durationMinutes);
}
```

---

## Flaw 9: No Circuit Breaker

**Problem**: Downstream failure → Kafka consumer thread starvation.

### Solution A — Resilience4j Circuit Breaker ⭐ Recommended

```gradle
implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.1.0'
```

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      loggingService:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 3
      aiService:
        sliding-window-size: 5
        failure-rate-threshold: 60
        wait-duration-in-open-state: 60s
  timelimiter:
    instances:
      loggingService:
        timeout-duration: 2s
      aiService:
        timeout-duration: 5s
```

```java
@CircuitBreaker(name = "loggingService", fallbackMethod = "fallbackHistory")
@TimeLimiter(name = "loggingService")
public CompletableFuture<List<UserLogEvent>> getAllEventsInDuration(String uuid, int duration) {
    return CompletableFuture.supplyAsync(() -> fetchFromGrpc(uuid, duration));
}

public CompletableFuture<List<UserLogEvent>> fallbackHistory(String uuid, int duration, Throwable t) {
    log.warn("Circuit open: using empty history for UUID {}", uuid);
    return CompletableFuture.completedFuture(Collections.emptyList());
}
```

---

### Solution B — gRPC Deadline + Retry

```java
public List<UserLogEvent> getAllEventsInDuration(String uuid, int duration) {
    try {
        return stub
            .withDeadlineAfter(2, TimeUnit.SECONDS)  // Hard timeout
            .getUserEvents(request)
            .getUserLogEventsList();
    } catch (StatusRuntimeException e) {
        if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
            log.warn("gRPC timeout for UUID: {}", uuid);
        }
        return Collections.emptyList();
    }
}
```

---

### Solution C — Bulkhead Pattern (Thread Isolation)

Isolate gRPC and AI calls in separate thread pools so one failure doesn't block the other:

```yaml
resilience4j:
  bulkhead:
    instances:
      grpcCalls:
        maxConcurrentCalls: 10
      aiCalls:
        maxConcurrentCalls: 5
```

---

## Flaw 10: `parallelStream()` Misuse

**Problem**: ForkJoinPool contention for trivial mapping.

### Solution: Use `stream()` instead

```diff
-List<GatewayLogEntity> entities = events.parallelStream()
+List<GatewayLogEntity> entities = events.stream()
     .map(event -> GatewayLogEntity.builder()
         // ... field mapping
         .build())
     .toList();
```

This is a one-line fix. No alternatives needed — `parallelStream()` is objectively wrong for a simple DTO→Entity mapping.

---

## Flaw 11: `record.toString()` Instead of JSON in Redis

**Problem**: Block metadata stored as Lombok toString format, not parseable JSON.

### Solution A — Jackson ObjectMapper ⭐ Recommended

```java
private final ObjectMapper objectMapper;

public void blockUser(String uuid, ThreatStrategy strategy) {
    BlockRecord record = BlockRecord.builder()
        .reason(strategy.getReason())
        .severity(determineSeverity(strategy.getBlockDuration()))
        .blockedAt(Instant.now().toEpochMilli())
        .expiresAt(Instant.now().plus(strategy.getBlockDuration()).toEpochMilli())
        .build();

    String json = objectMapper.writeValueAsString(record);
    redisTemplate.opsForValue()
        .set(BLACKLIST_PREFIX + uuid, json, strategy.getBlockDuration())
        .doOnError(e -> log.error("Failed to block: {}", e.getMessage()))
        .retry(3)
        .subscribe();
}
```

---

### Solution B — Custom Redis Serializer

Configure Redis to auto-serialize/deserialize Java objects:

```java
@Bean
public ReactiveRedisTemplate<String, BlockRecord> blockRecordRedisTemplate(
        ReactiveRedisConnectionFactory factory) {
    Jackson2JsonRedisSerializer<BlockRecord> serializer =
        new Jackson2JsonRedisSerializer<>(BlockRecord.class);
    RedisSerializationContext<String, BlockRecord> context =
        RedisSerializationContext.<String, BlockRecord>newSerializationContext(new StringRedisSerializer())
            .value(serializer)
            .build();
    return new ReactiveRedisTemplate<>(factory, context);
}
```

---

## Flaw 12: Single Points of Failure

**Problem**: Every component runs as a single instance.

### Solution A — Docker Compose Replicas

```yaml
services:
  api-gateway:
    deploy:
      replicas: 2
  mcp-server:
    deploy:
      replicas: 2
  logging-service:
    deploy:
      replicas: 2
```

---

### Solution B — Redis Sentinel / Cluster

```yaml
redis-master:
  image: redis:7.2.4
  command: redis-server --appendonly yes

redis-sentinel:
  image: redis:7.2.4
  command: redis-sentinel /etc/redis/sentinel.conf
  depends_on:
    - redis-master
```

---

### Solution C — Kafka Multi-Broker

```yaml
kafka-1:
  environment:
    KAFKA_BROKER_ID: 1
kafka-2:
  environment:
    KAFKA_BROKER_ID: 2
kafka-3:
  environment:
    KAFKA_BROKER_ID: 3
```

With `replication-factor: 3` for all topics.

---

### Solution D — Kubernetes (Production-Grade)

For true HA, deploy on Kubernetes:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
  template:
    spec:
      containers:
        - name: api-gateway
          image: shrihari7396/api-gateway:latest
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8079
```

---

## Quick Reference — Recommended Solution per Flaw

| Flaw | Recommended Solution | Effort |
|------|---------------------|--------|
| 1. Redundant DB fetches | Caffeine local cache (30s TTL) | 🟢 Low |
| 2. Re-analyze blocked users | Redis pre-check before analysis | 🟢 Low |
| 3. Sync LLM blocking | Async thread pool + two-phase pipeline | 🟡 Medium |
| 4. No deduplication | ConcurrentHashMap with 30s window | 🟢 Low |
| 5. Consumer bottleneck | Increase partitions + batch processing | 🟡 Medium |
| 6. Pipeline race condition | Co-consume `user-logs` in MCPService | 🟡 Medium |
| 7. Dashboard hits DB | Redis cache + database indexes | 🟡 Medium |
| 8. Missing gRPC server | Implement `@GrpcService` in LoggingService | 🔴 High |
| 9. No circuit breaker | Resilience4j + gRPC deadlines | 🟡 Medium |
| 10. parallelStream misuse | Replace with `stream()` | 🟢 Trivial |
| 11. toString vs JSON | Jackson `ObjectMapper` | 🟢 Low |
| 12. Single points of failure | Docker replicas + Redis Sentinel | 🔴 High |
