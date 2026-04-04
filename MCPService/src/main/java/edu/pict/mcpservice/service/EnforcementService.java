package edu.pict.mcpservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pict.mcpservice.model.BlockRecord;
import edu.pict.mcpservice.ports.BlockEnforcer;
import edu.pict.mcpservice.stratagies.blocking.ThreatStrategy;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnforcementService implements BlockEnforcer {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String LEGACY_BLACKLIST_PREFIX = "blacklist:";
    private static final String UUID_BLACKLIST_PREFIX = "blacklist:uuid:";
    private static final String IP_BLACKLIST_PREFIX = "blacklist:ip:";

    @Override
    public boolean isBlocked(String uuid) {
        Boolean blocked =
                redisTemplate
                        .hasKey(UUID_BLACKLIST_PREFIX + uuid)
                        .map(found -> Boolean.TRUE.equals(found))
                        .defaultIfEmpty(false)
                        .block();

        if (Boolean.TRUE.equals(blocked)) {
            return true;
        }

        Boolean blockedLegacy =
                redisTemplate
                        .hasKey(LEGACY_BLACKLIST_PREFIX + uuid)
                        .map(found -> Boolean.TRUE.equals(found))
                        .defaultIfEmpty(false)
                        .block();
        return Boolean.TRUE.equals(blockedLegacy);
    }

    @Override
    public void blockUser(String uuid, String clientIp, ThreatStrategy strategy) {
        String uuidKey = UUID_BLACKLIST_PREFIX + uuid;
        String legacyKey = LEGACY_BLACKLIST_PREFIX + uuid;
        Duration ttl = strategy.getBlockDuration();

        BlockRecord record =
                BlockRecord.builder()
                        .reason(strategy.getReason())
                        .severity(determineSeverity(ttl))
                        .blockedAt(Instant.now().toEpochMilli())
                        .expiresAt(Instant.now().plus(ttl).toEpochMilli())
                        .build();
        String payload = serializeRecord(record);

        redisTemplate
                .opsForValue()
                .set(uuidKey, payload, ttl)
                .flatMap(success -> redisTemplate.opsForValue().set(legacyKey, payload, ttl))
                .flatMap(
                        success -> {
                            if (clientIp == null || clientIp.isBlank()) {
                                return reactor.core.publisher.Mono.just(Boolean.TRUE);
                            }
                            return redisTemplate.opsForValue().set(IP_BLACKLIST_PREFIX + clientIp, payload, ttl);
                        })
                .doOnSuccess(
                        success ->
                                log.info(
                                        "🛡️ BLOCK EXECUTED: UUID {} | Strategy: {} | TTL: {}",
                                        uuid,
                                        strategy.getClass().getSimpleName(),
                                        ttl))
                .doOnError(
                        error ->
                                log.error(
                                        "Failed to persist block state for UUID {}: {}",
                                        uuid,
                                        error.getMessage()))
                .subscribe(); // Fire and forget
    }

    private String serializeRecord(BlockRecord record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize block record, falling back to minimal payload", e);
            return "{\"reason\":\"SERIALIZATION_ERROR\"}";
        }
    }

    private String determineSeverity(Duration ttl) {
        if (ttl.toHours() >= 24) return "CRITICAL";
        if (ttl.toHours() >= 1) return "MEDIUM";
        return "LOW";
    }
}
