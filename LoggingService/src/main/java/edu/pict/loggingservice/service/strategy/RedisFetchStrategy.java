package edu.pict.loggingservice.service.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pict.loggingservice.entity.GatewayLogEntity;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Fetches log events from Redis cache. Data is stored by the {@code RedisLogConsumer} as JSON
 * strings in a Redis list keyed by {@code log:events:{uuid}}.
 *
 * <p>This strategy supports the gRPC query parameters:
 *
 * <ul>
 *   <li><b>uuid</b> — used as the Redis key suffix for direct lookup
 *   <li><b>duration</b> (via {@code since} Instant) — used to filter cached entries by timestamp
 * </ul>
 *
 * <p>Returns an empty list on cache miss (key doesn't exist or no entries match the time window),
 * which signals the {@link LogFetchStrategyResolver} to fall back to the database strategy.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisFetchStrategy implements LogFetchStrategy {

    private static final String KEY_PREFIX = "log:events:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @SuppressWarnings("unchecked")
    public List<GatewayLogEntity> fetchLogs(String uuid, Instant since) {
        String key = KEY_PREFIX + uuid;

        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.FALSE.equals(exists)) {
            log.debug("Redis cache MISS for key={}", key);
            return Collections.emptyList();
        }

        List<String> jsonEntries = redisTemplate.opsForList().range(key, 0, -1);
        if (jsonEntries == null || jsonEntries.isEmpty()) {
            log.debug("Redis key={} exists but is empty", key);
            return Collections.emptyList();
        }

        long sinceEpochMs = since.toEpochMilli();

        List<GatewayLogEntity> result =
                jsonEntries.stream()
                        .map(
                                json -> {
                                    try {
                                        return objectMapper.readValue(json, Map.class);
                                    } catch (JsonProcessingException e) {
                                        log.warn("Failed to parse Redis entry: {}", json, e);
                                        return null;
                                    }
                                })
                        .filter(map -> map != null)
                        .filter(
                                map -> {
                                    Object ts = map.get("timestamp");
                                    if (ts instanceof Number) {
                                        return ((Number) ts).longValue() >= sinceEpochMs;
                                    }
                                    return false;
                                })
                        .map(this::mapToEntity)
                        .toList();

        log.debug(
                "Redis cache HIT for uuid={}: {} entries matched (since={})",
                uuid,
                result.size(),
                since);
        return result;
    }

    @Override
    public String strategyName() {
        return "REDIS";
    }

    @SuppressWarnings("unchecked")
    private GatewayLogEntity mapToEntity(Map<String, Object> map) {
        return GatewayLogEntity.builder()
                .id(UUID.randomUUID())
                .visitorId(getStr(map, "uuid"))
                .path(getStr(map, "path"))
                .method(getStr(map, "method"))
                .latencyMs(getLong(map, "latencyMs"))
                .queryParams(getStr(map, "queryParams"))
                .clientIp(getStr(map, "clientIp"))
                .statusCode(getInt(map, "statusCode"))
                .requestSize(getLong(map, "requestSize"))
                .occurredAt(Instant.ofEpochMilli(getLong(map, "timestamp")))
                .userAgent(getStr(map, "userAgent"))
                .build();
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Number ? ((Number) val).longValue() : 0L;
    }

    private int getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Number ? ((Number) val).intValue() : 0;
    }
}
