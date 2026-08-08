package edu.pict.loggingservice.service.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pict.loggingservice.entity.GatewayLogEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisFetchStrategyTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RedisFetchStrategy redisFetchStrategy;

    @Test
    void testFetchLogs_CacheMiss() {
        String uuid = "test-uuid";
        Instant since = Instant.now();

        when(redisTemplate.hasKey("log:events:" + uuid)).thenReturn(false);

        List<GatewayLogEntity> result = redisFetchStrategy.fetchLogs(uuid, since);

        assertTrue(result.isEmpty());
    }

    @Test
    void testFetchLogs_CacheHit_WithTimestampFiltering() throws JsonProcessingException {
        String uuid = "test-uuid";
        long now = System.currentTimeMillis();
        Instant since = Instant.ofEpochMilli(now - 10000);

        when(redisTemplate.hasKey("log:events:" + uuid)).thenReturn(true);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        String validJson = "{\"json\":\"valid\"}";
        String oldJson = "{\"json\":\"old\"}";
        
        when(listOperations.range("log:events:" + uuid, 0, -1)).thenReturn(List.of(validJson, oldJson));

        Map<String, Object> validMap = Map.of("uuid", uuid, "timestamp", now);
        Map<String, Object> oldMap = Map.of("uuid", uuid, "timestamp", now - 20000);

        when(objectMapper.readValue(validJson, Map.class)).thenReturn(validMap);
        when(objectMapper.readValue(oldJson, Map.class)).thenReturn(oldMap);

        List<GatewayLogEntity> result = redisFetchStrategy.fetchLogs(uuid, since);

        assertEquals(1, result.size());
        assertEquals(uuid, result.get(0).getVisitorId());
    }

    @Test
    void testFetchLogs_MalformedJson_IsIgnored() throws JsonProcessingException {
        String uuid = "test-uuid";
        Instant since = Instant.ofEpochMilli(System.currentTimeMillis() - 10000);

        when(redisTemplate.hasKey("log:events:" + uuid)).thenReturn(true);
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        String malformedJson = "{malformed}";
        when(listOperations.range("log:events:" + uuid, 0, -1)).thenReturn(List.of(malformedJson));

        when(objectMapper.readValue(malformedJson, Map.class))
                .thenThrow(new JsonProcessingException("Mock parse error") {});

        List<GatewayLogEntity> result = redisFetchStrategy.fetchLogs(uuid, since);

        // The malformed JSON should be skipped without crashing the app, returning an empty list
        assertTrue(result.isEmpty());
    }
}
