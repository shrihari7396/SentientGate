package edu.pict.mcpservice.service;

import edu.pict.mcpservice.grpc.UserLogEvent;
import edu.pict.mcpservice.kafkaEvents.LogEvent;
import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import edu.pict.mcpservice.ports.BlockEnforcer;
import edu.pict.mcpservice.ports.HistoryProvider;
import edu.pict.mcpservice.stratagies.blocking.AsyncThreatStrategy;
import edu.pict.mcpservice.stratagies.blocking.ThreatStrategy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpAnalysisService {

    private static final long DEDUP_WINDOW_MILLIS = Duration.ofSeconds(30).toMillis();
    private final Map<String, Long> recentlyProcessed = new ConcurrentHashMap<>();

    private final HistoryProvider historyProvider;
    private final BlockEnforcer blockEnforcer;
    private final AsyncThreatEvaluator asyncThreatEvaluator;
    // Spring automatically injects these sorted by @Order
    private final List<ThreatStrategy> strategies;

    public void analyze(SecurityAlertEvent alert) {
        if (alert.getUuid() == null || alert.getUuid().isBlank()) {
            log.warn("Skipping analysis due to missing UUID");
            return;
        }
        if (blockEnforcer.isBlocked(alert.getUuid())) {
            log.debug("⏭️ UUID {} already blocked, skipping analysis", alert.getUuid());
            return;
        }
        if (isDuplicate(alert)) {
            log.debug("⏭️ Duplicate alert within dedup window for UUID {}", alert.getUuid());
            return;
        }

        log.info("🔍 Analyzing threat for UUID: {}", alert.getUuid());

        // Context fetch: 10 min history from Logging Service
        List<UserLogEvent> grpcList = historyProvider.getAllEventsInDuration(alert.getUuid(), 10);
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

        List<ThreatStrategy> syncStrategies =
                strategies.stream().filter(s -> !(s instanceof AsyncThreatStrategy)).toList();
        List<AsyncThreatStrategy> asyncStrategies =
                strategies.stream()
                        .filter(AsyncThreatStrategy.class::isInstance)
                        .map(AsyncThreatStrategy.class::cast)
                        .toList();

        boolean matchedBySync =
                syncStrategies.stream()
                .filter(s -> s.isAvailable(alert, history))
                .findFirst()
                .map(
                        strategy -> {
                            log.warn(
                                    "🚫 Threat Detected! Strategy: {} | Reason: {}",
                                    strategy.getClass().getSimpleName(),
                                    strategy.getReason());
                            blockEnforcer.blockUser(alert.getUuid(), alert.getClientIp(), strategy);
                            return true;
                        })
                .orElse(false);

        if (!matchedBySync && !asyncStrategies.isEmpty()) {
            asyncThreatEvaluator.evaluate(alert, history, asyncStrategies);
        }
    }

    private boolean isDuplicate(SecurityAlertEvent alert) {
        String key =
                alert.getUuid()
                        + ":"
                        + alert.getErrorCode()
                        + ":"
                        + (alert.getAttemptedPath() == null ? "" : alert.getAttemptedPath());
        long now = System.currentTimeMillis();
        Long previous = recentlyProcessed.putIfAbsent(key, now);
        if (previous == null) {
            return false;
        }
        if (now - previous < DEDUP_WINDOW_MILLIS) {
            return true;
        }
        recentlyProcessed.put(key, now);
        return false;
    }
}
