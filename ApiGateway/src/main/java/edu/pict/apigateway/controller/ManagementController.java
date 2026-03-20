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
    private static final String BLACKLIST_PREFIX = "blacklist:";

    @GetMapping
    public Mono<ResponseEntity<List<String>>> getBlacklist() {
        return redisTemplate
                .keys(BLACKLIST_PREFIX + "*")
                .map(key -> key.substring(BLACKLIST_PREFIX.length()))
                .collectList()
                .map(ResponseEntity::ok);
    }

    @PostMapping("/{uuid}")
    public Mono<ResponseEntity<Void>> block(@PathVariable String uuid) {
        return redisTemplate
                .opsForValue()
                .set(BLACKLIST_PREFIX + uuid, "true")
                .map(success -> ResponseEntity.ok().<Void>build());
    }

    @DeleteMapping("/{uuid}")
    public Mono<ResponseEntity<Void>> unblock(@PathVariable String uuid) {
        return redisTemplate
                .delete(BLACKLIST_PREFIX + uuid)
                .map(count -> ResponseEntity.ok().<Void>build());
    }
}
