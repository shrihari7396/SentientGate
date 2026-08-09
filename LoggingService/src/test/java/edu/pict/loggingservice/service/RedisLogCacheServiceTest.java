package edu.pict.loggingservice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pict.loggingservice.kafka.model.GatewayDecisionEvent;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class RedisLogCacheServiceTest {

    @Mock private RedisTemplate<String, String> redisTemplate;

    @Mock private ListOperations<String, String> listOperations;

    @Mock private ObjectMapper objectMapper;

    @InjectMocks private RedisLogCacheService redisLogCacheService;

    @Test
    void testCacheEvents() throws JsonProcessingException {
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

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"json\":\"mock\"}");
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        redisLogCacheService.cacheEvents(events);

        verify(listOperations).leftPush("log:events:" + event.uuid(), "{\"json\":\"mock\"}");
        verify(redisTemplate).expire("log:events:" + event.uuid(), Duration.ofMinutes(10));
    }

    @Test
    void testCacheEvents_JsonProcessingException() throws JsonProcessingException {
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

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("Mock serialization error") {});

        // Should not throw an exception (gracefully caught)
        redisLogCacheService.cacheEvents(events);

        // Verify that Redis was never called because serialization failed
        verify(redisTemplate, org.mockito.Mockito.never()).opsForList();
    }
}
