package edu.pict.mcpservice.consumer;

import edu.pict.mcpservice.model.GatewayEvent;
import edu.pict.mcpservice.service.DecisionOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayEventConsumer {

    private final DecisionOrchestrator orchestrator;

    @KafkaListener(
            topics = "gateway-events",
            groupId = "mcp-server-group"
    )
    public void consume(GatewayEvent event) {
        log.info("Received GatewayEvent: {}", event);
        orchestrator.process(event);
    }
}
