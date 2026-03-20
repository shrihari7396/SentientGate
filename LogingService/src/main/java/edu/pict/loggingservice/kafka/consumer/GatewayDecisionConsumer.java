package edu.pict.loggingservice.kafka.consumer;

import edu.pict.loggingservice.kafka.model.GatewayDecisionEvent;
import edu.pict.loggingservice.service.KafkaBatchService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GatewayDecisionConsumer {

    private final KafkaBatchService kafkaBatchService;

    @KafkaListener(
            topics = "#{T(edu.pict.loggingservice.config.KafkaTopics).USER_LOGS.topic()}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(List<GatewayDecisionEvent> events) {
        log.info("Received gateway decision events: " + events);
        kafkaBatchService.consumeBatch(events);
    }
}
