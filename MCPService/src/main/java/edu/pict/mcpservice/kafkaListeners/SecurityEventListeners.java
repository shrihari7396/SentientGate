package edu.pict.mcpservice.kafkaListeners;

import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import edu.pict.mcpservice.service.McpAnalysisService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SecurityEventListeners {

    private final McpAnalysisService mcpAnalysisService;
    private final Executor analysisExecutor;

    public SecurityEventListeners(
            McpAnalysisService mcpAnalysisService,
            @Qualifier("analysisExecutor") Executor analysisExecutor) {
        this.mcpAnalysisService = mcpAnalysisService;
        this.analysisExecutor = analysisExecutor;
    }

    @KafkaListener(topics = "security-events", groupId = "mcp-analysis-group")
    public void onSecurityAlertBatch(List<SecurityAlertEvent> alerts) {
        log.info("📦 Kafka Batch Received: {} events — dispatching to analysis pool", alerts.size());

        Map<String, List<SecurityAlertEvent>> eventsByUuid =
                alerts.stream().collect(Collectors.groupingBy(SecurityAlertEvent::getUuid));

        // One task per UUID — no per-alert loop here, analyze() handles the entire batch
        eventsByUuid.forEach(
                (uuid, userAlerts) ->
                        analysisExecutor.execute(
                                () -> {
                                    try {
                                        mcpAnalysisService.analyze(uuid, userAlerts);
                                    } catch (Exception e) {
                                        log.error(
                                                "❌ Error during threat analysis for UUID: {}",
                                                uuid,
                                                e);
                                    }
                                }));
    }
}
