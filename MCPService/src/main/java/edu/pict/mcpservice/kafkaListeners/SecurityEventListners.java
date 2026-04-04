package edu.pict.mcpservice.kafkaListeners;

import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import edu.pict.mcpservice.service.McpAnalysisService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityEventListners {

    private final McpAnalysisService mcpAnalysisService;

    /**
     * This method listens to the 'security-events' topic. Whenever the ApiGateway sends an alert,
     * this triggers the Sentient Analysis.
     */
    @KafkaListener(topics = "security-events", groupId = "mcp-analysis-group")
    public void onSecurityAlert(List<SecurityAlertEvent> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            return;
        }
        Map<String, List<SecurityAlertEvent>> grouped =
                alerts.stream().collect(java.util.stream.Collectors.groupingBy(SecurityAlertEvent::getUuid));

        grouped.forEach(
                (uuid, events) -> {
                    SecurityAlertEvent representative =
                            events.stream()
                                    .max(Comparator.comparingInt(SecurityAlertEvent::getErrorCode))
                                    .orElse(events.get(0));
                    try {
                        mcpAnalysisService.analyze(representative);
                        log.info("✅ Analysis completed for UUID: {}", uuid);
                    } catch (Exception e) {
                        log.error("❌ Error during threat analysis for UUID: {}", uuid, e);
                    }
                });
    }
}
