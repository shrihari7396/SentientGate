package edu.pict.mcpservice.service;

import edu.pict.mcpservice.grpc.UserLogEvent;
import edu.pict.mcpservice.kafkaEvents.LogEvent;
import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import edu.pict.mcpservice.stratagies.blocking.ThreatStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpAnalysisService {

    private final EventHistoryService eventHistoryService;
    private final EnforcementService enforcementService;
    // Spring automatically injects these sorted by @Order
    private final List<ThreatStrategy> strategies;

    public void analyze(SecurityAlertEvent alert) {
        log.info("🔍 Analyzing threat for UUID: {}", alert.getUuid());

        // Context fetch: 10 min history from Logging Service
        List<UserLogEvent> grpcList = eventHistoryService.getAllEventsInDuration(alert.getUuid(), 10);
        List<LogEvent> history = grpcList.stream()
                .map(grpcEvent ->
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
                                .build()
                ).toList();

        // Functional pipeline to find the first matching strategy
        strategies.stream()
                .filter(s -> s.isAvailable(alert, history))
                .findFirst()
                .ifPresentOrElse(
                        strategy -> {
                            log.warn("🚫 Threat Detected! Strategy: {} | Reason: {}",
                                    strategy.getClass().getSimpleName(), strategy.getReason());

                            enforcementService.blockUser(
                                    alert.getUuid(),
                                    strategy
                            );
                        },
                        () -> log.info("✅ No malicious patterns found for UUID: {}", alert.getUuid())
                );
    }
}