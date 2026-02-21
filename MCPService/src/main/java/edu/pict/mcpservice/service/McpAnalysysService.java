package edu.pict.mcpservice.service;

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
        List<LogEvent> history = eventHistoryService.getAllEventsInDuration(alert.getUuid(), 10);

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
                                    strategy.getBlockDuration(),
                                    strategy.getReason()
                            );
                        },
                        () -> log.info("✅ No malicious patterns found for UUID: {}", alert.getUuid())
                );
    }
}