package edu.pict.loggingservice.kafka.consumer;

import edu.pict.loggingservice.entity.GatewayLogEntity;
import edu.pict.loggingservice.kafka.model.GatewayDecisionEvent;
import edu.pict.loggingservice.repository.GatewayLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class GatewayDecisionConsumer {

    private final GatewayLogRepository repository;

    @KafkaListener(
            topics = "sentientgate-decisions",
            groupId = "sentientgate-logging-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(GatewayDecisionEvent event) {

        GatewayLogEntity entity = GatewayLogEntity.builder()
                .id(event.eventId())
                .clientIp(event.clientIp())
                .routeId(event.routeId())
                .decision(event.decision())
                .statusCode(event.statusCode())
                .latencyMs(event.latencyMs())
                .occurredAt(Instant.ofEpochMilli(event.timestamp()))
                .build();

        repository.save(entity);
    }
}
