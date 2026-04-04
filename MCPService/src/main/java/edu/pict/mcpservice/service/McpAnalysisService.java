package edu.pict.mcpservice.service;

import edu.pict.mcpservice.grpc.UserLogEvent;
import edu.pict.mcpservice.kafkaEvents.LogEvent;
import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import edu.pict.mcpservice.stratagies.blocking.ThreatStrategy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import edu.pict.mcpservice.stratagies.blocking.AiAnomalyStrategy;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpAnalysisService {

    private final EventHistoryService eventHistoryService;
    private final EnforcementService enforcementService;
    private final List<ThreatStrategy> strategies;
    
    private final ConcurrentHashMap<String, Long> recentlyProcessed = new ConcurrentHashMap<>();

    public void analyze(SecurityAlertEvent alert) {
        // Skip if already blocked (Flaw 2)
        if (Boolean.TRUE.equals(enforcementService.isBlocked(alert.getUuid()))) {
            log.info("⏭️ UUID {} already blocked, skipping analysis", alert.getUuid());
            return;
        }

        // Event Deduplication (Flaw 4)
        String dedupKey = alert.getUuid() + ":" + alert.getErrorCode();
        Long lastProcessed = recentlyProcessed.get(dedupKey);

        if (lastProcessed != null && (System.currentTimeMillis() - lastProcessed) < 30_000) {
            log.debug("⏭️ Dedup: skipping duplicate event for {}", dedupKey);
            return;
        }
        recentlyProcessed.put(dedupKey, System.currentTimeMillis());

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

        // Flaw 3: Run AI analysis asynchronously outside the Kafka consumer thread
        strategies.stream()
                .filter(s -> s instanceof AiAnomalyStrategy)
                .findFirst()
                .ifPresent(
                        aiStrategy ->
                                CompletableFuture.runAsync(
                                        () -> {
                                            try {
                                                if (aiStrategy.isAvailable(alert, history)) {
                                                    log.warn("🚫 [AI] Threat Detected! Strategy: {}", aiStrategy.getClass().getSimpleName());
                                                    enforcementService.blockUser(alert.getUuid(), aiStrategy);
                                                }
                                            } catch (Exception e) {
                                                log.error("AI Analysis failed asynchronously", e);
                                            }
                                        }));

        // Functional pipeline to find the first matching synchronous strategy
        strategies.stream()
                .filter(s -> !(s instanceof AiAnomalyStrategy))
                .filter(s -> s.isAvailable(alert, history))
                .findFirst()
                .ifPresentOrElse(
                        strategy -> {
                            log.warn(
                                    "🚫 Threat Detected! Strategy: {} | Reason: {}",
                                    strategy.getClass().getSimpleName(),
                                    strategy.getReason());

                            enforcementService.blockUser(alert.getUuid(), strategy);
                        },
                        () ->
                                log.info(
                                        "✅ No synchronous malicious patterns found for UUID: {}",
                                        alert.getUuid()));
    }
}
