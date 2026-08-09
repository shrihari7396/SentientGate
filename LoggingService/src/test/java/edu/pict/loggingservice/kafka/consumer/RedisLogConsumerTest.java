package edu.pict.loggingservice.kafka.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import edu.pict.loggingservice.kafka.model.GatewayDecisionEvent;
import edu.pict.loggingservice.service.RedisLogCacheService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RedisLogConsumerTest {

    @Mock private RedisLogCacheService redisLogCacheService;

    @InjectMocks private RedisLogConsumer redisLogConsumer;

    @Test
    void testConsumeSuccess() {
        GatewayDecisionEvent event =
                new GatewayDecisionEvent(
                        UUID.randomUUID().toString(),
                        "/api/test",
                        "GET",
                        "127.0.0.1",
                        "route-1",
                        "ALLOW",
                        200,
                        100L,
                        50L,
                        "",
                        "test-agent",
                        System.currentTimeMillis());
        List<GatewayDecisionEvent> events = List.of(event);

        redisLogConsumer.consume(events);

        verify(redisLogCacheService).cacheEvents(events);
    }

    @Test
    void testConsumeExceptionHandledGracefully() {
        GatewayDecisionEvent event =
                new GatewayDecisionEvent(
                        UUID.randomUUID().toString(),
                        "/api/test",
                        "GET",
                        "127.0.0.1",
                        "route-1",
                        "ALLOW",
                        200,
                        100L,
                        50L,
                        "",
                        "test-agent",
                        System.currentTimeMillis());
        List<GatewayDecisionEvent> events = List.of(event);

        doThrow(new RuntimeException("Redis connection error"))
                .when(redisLogCacheService)
                .cacheEvents(any());

        // Should not throw an exception (gracefully caught)
        redisLogConsumer.consume(events);

        verify(redisLogCacheService).cacheEvents(events);
    }
}
