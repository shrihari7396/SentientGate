package edu.pict.loggingservice.service;

import edu.pict.loggingservice.entity.GatewayLogEntity;
import edu.pict.loggingservice.kafka.model.GatewayDecisionEvent;
import edu.pict.loggingservice.repository.GatewayLogRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class KafkaBatchService {
    private final GatewayLogRepository gatewayLogRepository;

    public void consumeBatch(List<GatewayDecisionEvent> events) {

        List<GatewayLogEntity> entities = events.stream()
                .map(event -> GatewayLogEntity.builder()
                        .id(UUID.randomUUID())
                        .clientIp(event.clientIp())
                        .routeId(event.routeId())
                        .decision(event.decision())
                        .statusCode(event.statusCode())
                        .latencyMs(event.latencyMs())
                        .occurredAt(Instant.ofEpochMilli(event.timestamp()))
                        .build())
                .toList();
        gatewayLogRepository.saveAll(entities);
    }

}
