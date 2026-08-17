package edu.pict.loggingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pict.loggingservice.kafka.model.GatewayDecisionEvent;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Caches log events into Redis for near-instant gRPC reads. Each event is stored as a JSON string
 * in a Redis list keyed by the visitor UUID: {@code log:events:{uuid}}.
 *
 * <p>The JSON structure matches the gRPC {@code UserLogEvent} proto fields (uuid, path, method,
 * latencyMs, queryParams, clientIp, statusCode, requestSize, timestamp, userAgent) so the {@link
 * edu.pict.loggingservice.service.strategy.RedisFetchStrategy} can deserialize and return them
 * directly — queryable by the gRPC request parameters (uuid + duration).
 *
 * <p>Each key has a sliding 10-minute TTL that resets on every new event write.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLogCacheService {

    private static final String KEY_PREFIX = "log:events:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void cacheEvents(List<GatewayDecisionEvent> events) {
        for (GatewayDecisionEvent event : events) {
            String key = KEY_PREFIX + event.uuid();
            try {
                String json = objectMapper.writeValueAsString(toRedisMap(event));
                redisTemplate.opsForList().leftPush(key, json);
                redisTemplate.expire(key, TTL);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize log event for uuid={}", event.uuid(), e);
            }
        }
        log.debug("Cached {} events to Redis", events.size());
    }

    /**
     * Builds a map whose keys exactly match the gRPC UserLogEvent proto fields, so the
     * RedisFetchStrategy can query and return them using the gRPC request parameters (uuid for key
     * lookup, duration/timestamp for filtering).
     */
    private Map<String, Object> toRedisMap(GatewayDecisionEvent event) {
        Map<String, Object> map = new HashMap<>();
        map.put("uuid", event.uuid());
        map.put("path", event.path());
        map.put("method", event.method());
        map.put("latencyMs", event.latencyMs());
        map.put("queryParams", event.queryParams());
        map.put("clientIp", event.clientIp());
        map.put("statusCode", event.statusCode());
        map.put("requestSize", event.requestSize());
        map.put("timestamp", event.timestamp());
        map.put("userAgent", event.userAgent());
        return map;
    }
}
