# MCPService

MCPService is the real-time threat analysis engine in SentientGate.
It consumes `security-events` from Kafka, enriches alerts with recent user history via gRPC, applies ordered blocking strategies, and writes block decisions to Redis with TTL.

## What This Service Does

- Consumes batched `SecurityAlertEvent` messages from Kafka.
- Deduplicates repeated alerts using Redis to support horizontal scaling.
- Fetches user request history from `LOGGING-SERVICE` (gRPC) with Redis-backed history cache.
- Runs low-cost rule-based strategies first.
- Runs AI anomaly strategy only if no rule-based strategy matched.
- Persists block decisions in Redis (`blacklist:{uuid}`) with per-strategy TTL.

## High-Level Architecture

`Kafka (security-events)` -> `SecurityEventListeners` -> `McpAnalysisService` -> `ThreatStrategy[]` -> `EnforcementService (Redis blacklist)`

`McpAnalysisService` -> `EventHistoryService` -> `Redis history cache` -> `gRPC LOGGING-SERVICE`

`AiAnomalyStrategy` -> `AIClient` -> `Feign ai-inference-service`

## Runtime Flow

1. Kafka batch arrives in [`SecurityEventListeners`](/home/shrihari/Documents/PersonalProjects/SentientGate/MCPService/src/main/java/edu/pict/mcpservice/kafkaListeners/SecurityEventListeners.java).
2. Alerts are grouped by UUID and processed sequentially per UUID within the batch.
3. [`McpAnalysisService.analyze(...)`](/home/shrihari/Documents/PersonalProjects/SentientGate/MCPService/src/main/java/edu/pict/mcpservice/service/McpAnalysisService.java) performs:
4. Block short-circuit: skip if UUID already blocked.
5. Dedup short-circuit: Redis `setIfAbsent` on `mcp:dedup:{uuid}:{errorCode}` with 30s TTL.
6. History load: fetch `last 10 min` history via [`EventHistoryService`](/home/shrihari/Documents/PersonalProjects/SentientGate/MCPService/src/main/java/edu/pict/mcpservice/service/EventHistoryService.java).
7. Rule strategies run first (non-AI) and stop at first match.
8. If matched: write block record to Redis and return immediately.
9. If no rule matched: run AI strategy asynchronously.
10. If AI matches with confidence threshold, write block record to Redis.

## Strategy Order and Cost Model

Strategies implement [`ThreatStrategy`](/home/shrihari/Documents/PersonalProjects/SentientGate/MCPService/src/main/java/edu/pict/mcpservice/stratagies/blocking/ThreatStrategy.java) and are ordered with `@Order`.

| Order | Strategy | Trigger | Block TTL | Reason |
|---|---|---|---|---|
| 1 | `PatternMatchStrategy` | Known malicious path/injection patterns | 1 day | `CRITICAL_INJECTION_ATTEMPT` |
| 2 | `SensitivePathStrategy` | Access to sensitive/recon paths | 7 days | `SENSITIVE_PATH_RECONNAISSANCE` |
| 3 | `RateLimitCoolDownStrategy` | Alert error code is `429` | 15 minutes | `Aggressive polling detected. 15m cool-down.` |
| 4 | `HighErrorRateStrategy` | >70% errors with enough history | 2 hours | `HIGH_ERROR_RATE_SCANNER_DETECTED` |
| 5 | `BurstTrafficStrategy` | >=20 requests in <5 seconds | 30 minutes | `BURST_TRAFFIC_DETECTED_BOT_SUSPECT` |
| 6 | `AiAnomalyStrategy` | AI says anomaly and confidence >0.85 | 6 hours | `AI_BEHAVIORAL_ANOMALY_DETECTED` |

Cost optimization currently implemented:

- AI is executed only after all cheaper rule strategies fail.
- First matching rule strategy stops further analysis immediately.
- Redis dedup and block checks avoid repeated work.

## Redis Usage (Distributed State)

This service is horizontally scalable because critical state is in Redis.

| Key Pattern | Purpose | TTL |
|---|---|---|
| `blacklist:{uuid}` | Active block record (JSON with reason/severity/timestamps) | Strategy-dependent |
| `mcp:dedup:{uuid}:{errorCode}` | Dedup repeated alert processing | 30 seconds |
| `mcp:history:{uuid}:{duration}` | Cached gRPC user history payload | 30 seconds |

Notes:

- History payload is stored as Base64-encoded protobuf bytes.
- `EnforcementService` writes blacklist entries using `ReactiveRedisTemplate`.
- `McpAnalysisService` and `EventHistoryService` use `StringRedisTemplate`.

## Integrations

- Kafka topic: `security-events`
- Eureka service discovery
- gRPC client target: `LOGGING-SERVICE`
- Feign client target: `ai-inference-service`

## Configuration

Main config file: [`application.yml`](/home/shrihari/Documents/PersonalProjects/SentientGate/MCPService/src/main/resources/application.yml)

Important properties:

- `server.port=9991`
- `server.servlet.context-path=/mcp-service`
- `spring.data.redis.host=${SPRING_DATA_REDIS_HOST:localhost}`
- `spring.data.redis.port=${SPRING_DATA_REDIS_PORT:6379}`
- `spring.kafka.bootstrap-servers=localhost:9092`
- `spring.kafka.listener.type=batch`
- `spring.kafka.listener.concurrency=3`
- `grpc.client.logging-service.address=discovery:///LOGGING-SERVICE`

## Build and Run

Requirements:

- Java 21 (project toolchain)
- Redis
- Kafka
- Eureka (for service discovery)

From `MCPService/`:

```bash
./gradlew clean test
./gradlew bootRun
```

Docker image is defined in [`Dockerfile`](/home/shrihari/Documents/PersonalProjects/SentientGate/MCPService/Dockerfile) and uses JDK/JRE 21.

## Testing

Current tests cover:

- Rule strategies:
- `PatternMatchStrategyTest`
- `SensitivePathStrategyTest`
- `HighErrorRateStrategyTest`
- `BurstTrafficStrategyTest`
- Core analysis flow:
- `McpAnalysisServiceTest`

Coverage gaps to be aware of:

- Redis dedup behavior in `McpAnalysisService` is not directly unit-tested.
- Redis history cache behavior in `EventHistoryService` is not directly unit-tested.
- AI async execution path lacks focused tests.

## Operational Caveats Found During Analysis

1. `@KafkaListener` sets `groupId = "mcp-analysis-group"` while `application.yml` sets `spring.kafka.consumer.group-id = sentientgate-mcp-service`.
The annotation-level group id takes precedence for that listener.

2. `EnforcementService.blockUser(...)` currently logs successful block writes with `log.error(...)` instead of `log.info(...)`/`log.warn(...)`.
This can inflate error-rate dashboards.

3. `RedisConfig` defines a generic `RedisTemplate<String, Object>` bean, but the service logic primarily uses `StringRedisTemplate` and `ReactiveRedisTemplate<String, String>`.
The custom bean is not central to the current flow.

4. `McpAnalysisServiceTest` constructor usage may need updating if dependencies change further (it currently does not model Redis-template-backed dedup path).

## Suggested Next Improvements

- Add unit tests for Redis dedup hit/miss and Redis history cache hit/miss.
- Add metrics counters: dedup skips, rule hits by strategy, AI invocations, block writes.
- Add AI cooldown key (Redis) to avoid repeated AI calls per UUID in short windows.
- Normalize Kafka consumer group-id configuration to one source of truth.

## Key Source Files

- App bootstrap: [`McpServiceApplication.java`](/home/shrihari/Documents/PersonalProjects/SentientGate/MCPService/src/main/java/edu/pict/mcpservice/McpServiceApplication.java)
- Kafka listener: [`SecurityEventListeners.java`](/home/shrihari/Documents/PersonalProjects/SentientGate/MCPService/src/main/java/edu/pict/mcpservice/kafkaListeners/SecurityEventListeners.java)
- Analysis orchestrator: [`McpAnalysisService.java`](/home/shrihari/Documents/PersonalProjects/SentientGate/MCPService/src/main/java/edu/pict/mcpservice/service/McpAnalysisService.java)
- History + cache: [`EventHistoryService.java`](/home/shrihari/Documents/PersonalProjects/SentientGate/MCPService/src/main/java/edu/pict/mcpservice/service/EventHistoryService.java)
- Enforcement: [`EnforcementService.java`](/home/shrihari/Documents/PersonalProjects/SentientGate/MCPService/src/main/java/edu/pict/mcpservice/service/EnforcementService.java)
- AI client: [`AIClient.java`](/home/shrihari/Documents/PersonalProjects/SentientGate/MCPService/src/main/java/edu/pict/mcpservice/service/AIClient.java)
- Strategies: `/src/main/java/edu/pict/mcpservice/stratagies/blocking/`
- Proto contract: [`user_log_event.proto`](/home/shrihari/Documents/PersonalProjects/SentientGate/MCPService/src/main/proto/user_log_event.proto)
