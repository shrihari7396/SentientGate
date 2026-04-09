package edu.pict.mcpservice.service;

import edu.pict.mcpservice.grpc.UserLogEvent;
import edu.pict.mcpservice.kafkaEvents.LogEvent;
import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import edu.pict.mcpservice.stratagies.blocking.AiAnomalyStrategy;
import edu.pict.mcpservice.stratagies.blocking.ThreatStrategy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpAnalysisService {

    private static final String DEDUP_PREFIX = "mcp:dedup:";
    private static final Duration DEDUP_WINDOW = Duration.ofSeconds(30);

    private final EventHistoryService eventHistoryService;
    private final EnforcementService enforcementService;
    private final List<ThreatStrategy> strategies;
    private final StringRedisTemplate stringRedisTemplate;

    public void analyze(SecurityAlertEvent alert) {
        // Skip if already blocked (Flaw 2)
        if (Boolean.TRUE.equals(enforcementService.isBlocked(alert.getUuid()))) {
            log.info("⏭️ UUID {} already blocked, skipping analysis", alert.getUuid());
            return;
        }

        // Event Deduplication (Flaw 4)
        String dedupKey = DEDUP_PREFIX + alert.getUuid() + ":" + alert.getErrorCode();
        Boolean isFirstSeen =
                stringRedisTemplate
                        .opsForValue()
                        .setIfAbsent(dedupKey, String.valueOf(System.currentTimeMillis()), DEDUP_WINDOW);
        if (!Boolean.TRUE.equals(isFirstSeen)) {
            log.debug("⏭️ Dedup: skipping duplicate event for {}", dedupKey);
            return;
        }

        log.info("🔍 Analyzing threat for UUID: {}", alert.getUuid());

        // Context fetch: 10 min history from Logging Service
        List<UserLogEvent> grpcList =
                eventHistoryService.getAllEventsInDuration(alert.getUuid(), 10);
        List<LogEvent> history =
                grpcList.stream()
                        .map(
                                grpcEvent ->
                                        LogEvent.builder()
                                                .uuid(grpcEvent.getUuid())
                                                .path(grpcEvent.getPath())
                                                .method(grpcEvent.getMethod())
                                                .latencyMs(grpcEvent.getLatencyMs())
                                                .queryParams(grpcEvent.getQueryParams())
                                                .clientIp(grpcEvent.getClientIp())
                                                .statusCode(grpcEvent.getStatusCode())
                                                .requestSize(grpcEvent.getRequestSize())
                                                .timestamp(grpcEvent.getTimestamp())
                                                .userAgent(grpcEvent.getUserAgent())
                                                .build())
                        .toList();

        // First matching synchronous strategy wins; stop further analysis immediately.
        var matchedSyncStrategy =
                strategies.stream()
                        .filter(s -> !(s instanceof AiAnomalyStrategy))
                        .filter(s -> s.isAvailable(alert, history))
                        .findFirst();

        if (matchedSyncStrategy.isPresent()) {
            ThreatStrategy strategy = matchedSyncStrategy.get();
            log.warn(
                    "🚫 Threat Detected! Strategy: {} | Reason: {}",
                    strategy.getClass().getSimpleName(),
                    strategy.getReason());
            enforcementService.blockUser(alert.getUuid(), strategy);
            return;
        }

        log.info("✅ No synchronous malicious patterns found for UUID: {}", alert.getUuid());

        // Run AI analysis only if no synchronous strategy blocked the user.
        strategies.stream()
                .filter(s -> s instanceof AiAnomalyStrategy)
                .findFirst()
                .ifPresent(
                        aiStrategy ->
                                CompletableFuture.runAsync(
                                        () -> {
                                            try {
                                                if (aiStrategy.isAvailable(alert, history)) {
                                                    log.warn(
                                                            "🚫 [AI] Threat Detected! Strategy: {}",
                                                            aiStrategy.getClass().getSimpleName());
                                                    enforcementService.blockUser(
                                                            alert.getUuid(), aiStrategy);
                                                }
                                            } catch (Exception e) {
                                                log.error("AI Analysis failed asynchronously", e);
                                            }
                                        }));
    }
}
