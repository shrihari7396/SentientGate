package edu.pict.loggingservice.kafka.consumer;

import edu.pict.loggingservice.kafka.model.GatewayDecisionEvent;
import edu.pict.loggingservice.service.RedisLogCacheService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Independent Kafka consumer (group: logging-redis-writer) that writes log events to Redis
 * immediately upon receipt. This consumer resolves the race condition where MCPService queries gRPC
 * for logs before the slower PostgreSQL batch insert completes — Redis writes finish in ~1ms,
 * making data available for gRPC queries almost instantly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisLogConsumer {

    private final RedisLogCacheService redisLogCacheService;

    @KafkaListener(
            topics = "#{T(edu.pict.loggingservice.config.KafkaTopics).USER_LOGS.topic()}",
            containerFactory = "redisKafkaListenerContainerFactory")
    public void consume(List<GatewayDecisionEvent> events) {
        log.debug("Redis consumer received {} log events", events.size());
        try {
            redisLogCacheService.cacheEvents(events);
        } catch (Exception e) {
            log.warn(
                    "Failed to cache {} events to Redis — DB consumer will persist them",
                    events.size(),
                    e);
        }
    }
}
