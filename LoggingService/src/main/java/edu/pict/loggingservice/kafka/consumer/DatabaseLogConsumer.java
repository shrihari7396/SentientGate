package edu.pict.loggingservice.kafka.consumer;

import edu.pict.loggingservice.kafka.model.GatewayDecisionEvent;
import edu.pict.loggingservice.service.KafkaBatchService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Independent Kafka consumer (group: logging-db-writer) that batch-inserts log events into
 * PostgreSQL for durable, long-term storage. Operates independently from the Redis consumer so each
 * can process at its own pace without blocking the other.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseLogConsumer {

    private final KafkaBatchService kafkaBatchService;

    @KafkaListener(
            topics = "#{T(edu.pict.loggingservice.config.KafkaTopics).USER_LOGS.topic()}",
            containerFactory = "dbKafkaListenerContainerFactory")
    public void consume(List<GatewayDecisionEvent> events) {
        log.info("DB consumer received {} log events for persistence", events.size());
        kafkaBatchService.consumeBatch(events);
    }
}
