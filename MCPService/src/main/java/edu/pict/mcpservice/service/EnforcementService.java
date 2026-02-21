package edu.pict.mcpservice.service;

import edu.pict.mcpservice.model.BlockRecord;
import edu.pict.mcpservice.stratagies.blocking.ThreatStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnforcementService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";

    /**
     * Executes the block by writing to Redis with a TTL.
     */
    public void blockUser(String uuid, ThreatStrategy strategy) {
        String key = BLACKLIST_PREFIX + uuid;
        Duration ttl = strategy.getBlockDuration();

        BlockRecord record = BlockRecord.builder()
                .reason(strategy.getReason())
                .severity(determineSeverity(ttl))
                .blockedAt(Instant.now().toEpochMilli())
                .expiresAt(Instant.now().plus(ttl).toEpochMilli())
                .build();

        // Convert to JSON and save
        redisTemplate.opsForValue()
                .set(key, record.toString(), ttl)
                .doOnSuccess(success -> log.error("🛡️ SENTENCE EXECUTED: UUID {} | Strategy: {} | TTL: {}",
                        uuid, strategy.getClass().getSimpleName(), ttl))
                .subscribe(); // Fire and forget
    }

    private String determineSeverity(Duration ttl) {
        if (ttl.toHours() >= 24) return "CRITICAL";
        if (ttl.toHours() >= 1) return "MEDIUM";
        return "LOW";
    }
}