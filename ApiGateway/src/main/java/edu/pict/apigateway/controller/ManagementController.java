package edu.pict.apigateway.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/mgmt/blacklist")
@RequiredArgsConstructor
public class ManagementController {

    private final ReactiveStringRedisTemplate redisTemplate;
    private static final String UUID_BLACKLIST_PREFIX = "blacklist:uuid:";
    private static final String LEGACY_BLACKLIST_PREFIX = "blacklist:";

    @GetMapping
    public Mono<ResponseEntity<List<String>>> getBlacklist() {
        return redisTemplate
                .keys(UUID_BLACKLIST_PREFIX + "*")
                .map(key -> key.substring(UUID_BLACKLIST_PREFIX.length()))
                .collectList()
                .map(ResponseEntity::ok);
    }

    @PostMapping("/{uuid}")
    public Mono<ResponseEntity<Void>> block(@PathVariable String uuid) {
        return redisTemplate
                .opsForValue()
                .set(UUID_BLACKLIST_PREFIX + uuid, "true")
                .map(success -> ResponseEntity.ok().<Void>build());
    }

    @DeleteMapping("/{uuid}")
    public Mono<ResponseEntity<Void>> unblock(@PathVariable String uuid) {
        return redisTemplate
                .delete(UUID_BLACKLIST_PREFIX + uuid)
                .flatMap(count -> redisTemplate.delete(LEGACY_BLACKLIST_PREFIX + uuid))
                .map(count -> ResponseEntity.ok().<Void>build());
    }
}
