package edu.pict.mcpservice.kafkaListeners;

import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import edu.pict.mcpservice.service.McpAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityEventListners {

    private final McpAnalysisService mcpAnalysisService;

    /**
     * This method listens to the 'security-events' topic.
     * Whenever the ApiGateway sends an alert, this triggers the Sentient Analysis.
     */
    @KafkaListener(topics = "security-events", groupId = "mcp-analysis-group")
    public void onSecurityAlert(SecurityAlertEvent alert) {
        log.info("🔔 Kafka Event Received: UUID={} | ErrorCode={} | Reason={}",
                alert.getUuid(), alert.getErrorCode(), alert.getReason());

        try {
            // Passing the alert to our MCP Analysis engine
            mcpAnalysisService.analyze(alert);
            log.info("✅ Analysis completed for UUID: {}", alert.getUuid());
        } catch (Exception e) {
            log.error("❌ Error during threat analysis for UUID: {}", alert.getUuid(), e);
        }
    }
}