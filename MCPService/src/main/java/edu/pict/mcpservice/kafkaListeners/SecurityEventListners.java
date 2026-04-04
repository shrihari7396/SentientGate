package edu.pict.mcpservice.kafkaListeners;

import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import edu.pict.mcpservice.service.McpAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityEventListners {

    private final McpAnalysisService mcpAnalysisService;

    /**
     * This method listens to the 'security-events' topic in batch. Whenever the ApiGateway sends alerts,
     * this triggers the Sentient Analysis.
     */
    @KafkaListener(topics = "security-events", groupId = "mcp-analysis-group")
    public void onSecurityAlertBatch(List<SecurityAlertEvent> alerts) {
        log.info("📦 Kafka Batch Received: {} events", alerts.size());

        // Group by UUID to possibly process events for the same UUID sequentially and efficiently
        Map<String, List<SecurityAlertEvent>> eventsByUuid = alerts.stream()
                .collect(Collectors.groupingBy(SecurityAlertEvent::getUuid));

        eventsByUuid.forEach((uuid, userAlerts) -> {
            log.info("Processing {} events for UUID: {}", userAlerts.size(), uuid);
            for (SecurityAlertEvent alert : userAlerts) {
                try {
                    mcpAnalysisService.analyze(alert);
                } catch (Exception e) {
                    log.error("❌ Error during threat analysis for UUID: {}", alert.getUuid(), e);
                }
            }
        });
    }
}
