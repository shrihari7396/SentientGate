package edu.pict.loggingservice.kafka.consumer;

import edu.pict.loggingservice.entity.GatewayLogEntity;
import edu.pict.loggingservice.kafka.model.GatewayDecisionEvent;
import edu.pict.loggingservice.repository.GatewayLogRepository;
import edu.pict.loggingservice.service.KafkaBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GatewayDecisionConsumer {

    private final KafkaBatchService kafkaBatchService;

    @KafkaListener(
            topics = "sentientgate-decisions",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(List<GatewayDecisionEvent> events) {
        kafkaBatchService.consumeBatch(events);
    }
}
