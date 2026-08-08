package edu.pict.mcpservice.util;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service responsible for Redis-based deduplication and guard checks
 * to prevent redundant processing in MCP Analysis.
 */
@Slf4j
@Service
public class RedisGuardService {

    private static final String DEDUP_PREFIX = "mcp:dedup:";
    private static final String CHECKED_PREFIX = "mcp:checked:";
    private static final Duration DEDUP_WINDOW = Duration.ofSeconds(30);
    private static final Duration CHECKED_WINDOW = Duration.ofSeconds(200);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisGuardService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Returns true if this UUID was already processed by MCP within the checked window.
     */
    public boolean wasRecentlyChecked(String uuid) {
        String checkedKey = CHECKED_PREFIX + uuid;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(checkedKey))) {
            log.debug("UUID {} was recently checked by MCP, skipping", uuid);
            return true;
        }
        return false;
    }

    /**
     * Marks this UUID as checked so subsequent batches within the window are skipped.
     */
    public void markAsChecked(String uuid) {
        String checkedKey = CHECKED_PREFIX + uuid;
        stringRedisTemplate
                .opsForValue()
                .set(checkedKey, String.valueOf(System.currentTimeMillis()), CHECKED_WINDOW);
    }

    /**
     * Returns true if this exact uuid+errorCode combination was NOT seen recently (first time).
     */
    public boolean isFirstOccurrence(String uuid, int errorCode) {
        String dedupKey = DEDUP_PREFIX + uuid + ":" + errorCode;
        Boolean isFirstSeen =
                stringRedisTemplate
                        .opsForValue()
                        .setIfAbsent(
                                dedupKey,
                                String.valueOf(System.currentTimeMillis()),
                                DEDUP_WINDOW);

        if (!Boolean.TRUE.equals(isFirstSeen)) {
            log.debug("Dedup: skipping duplicate event for {}", dedupKey);
            return false;
        }
        return true;
    }
}
