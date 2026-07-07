package edu.pict.apigateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping({"/api/mgmt/blacklist", "/api/threat/blacklist"})
@RequiredArgsConstructor
public class ManagementController {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String BLACKLIST_PREFIX = "blacklist:";

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BlockRecord {
        private String reason;
        private String severity;
        private long blockedAt;
        private long expiresAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BlacklistEntry {
        private String uuid;
        private String reason;
        private String blockedAt;
        private long ttlSeconds;
    }

    @GetMapping
    public Mono<ResponseEntity<List<BlacklistEntry>>> getBlacklist() {
        return redisTemplate
                .keys(BLACKLIST_PREFIX + "*")
                .flatMap(key -> {
                    String uuid = key.substring(BLACKLIST_PREFIX.length());
                    return Mono.zip(
                        redisTemplate.opsForValue().get(key),
                        redisTemplate.getExpire(key)
                    ).map(tuple -> {
                        String json = tuple.getT1();
                        Duration expire = tuple.getT2();
                        long ttlSeconds = expire != null ? expire.getSeconds() : 3600;

                        String reason = "AI_ESCALATION";
                        String blockedAt = Instant.now().toString();

                        try {
                            BlockRecord record = objectMapper.readValue(json, BlockRecord.class);
                            reason = record.getReason();
                            blockedAt = Instant.ofEpochMilli(record.getBlockedAt()).toString();
                        } catch (Exception e) {
                            reason = "MANUAL_BLOCK";
                            blockedAt = Instant.now().toString();
                        }

                        return BlacklistEntry.builder()
                                .uuid(uuid)
                                .reason(reason)
                                .blockedAt(blockedAt)
                                .ttlSeconds(ttlSeconds)
                                .build();
                    });
                })
                .collectList()
                .map(ResponseEntity::ok);
    }

    @PostMapping("/{uuid}")
    public Mono<ResponseEntity<Void>> block(@PathVariable String uuid) {
        BlockRecord record = BlockRecord.builder()
                .reason("MANUAL_BLOCK")
                .severity("MEDIUM")
                .blockedAt(Instant.now().toEpochMilli())
                .expiresAt(Instant.now().plus(Duration.ofHours(1)).toEpochMilli())
                .build();
        try {
            String jsonVal = objectMapper.writeValueAsString(record);
            return redisTemplate
                    .opsForValue()
                    .set(BLACKLIST_PREFIX + uuid, jsonVal, Duration.ofHours(1))
                    .map(success -> ResponseEntity.ok().<Void>build());
        } catch (Exception e) {
            return redisTemplate
                    .opsForValue()
                    .set(BLACKLIST_PREFIX + uuid, "true", Duration.ofHours(1))
                    .map(success -> ResponseEntity.ok().<Void>build());
        }
    }

    @DeleteMapping("/{uuid}")
    public Mono<ResponseEntity<Void>> unblock(@PathVariable String uuid) {
        return redisTemplate
                .delete(BLACKLIST_PREFIX + uuid)
                .map(count -> ResponseEntity.ok().<Void>build());
    }
}
