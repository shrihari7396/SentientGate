package edu.pict.apigateway.service.impl;

import edu.pict.apigateway.service.JwtBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisJwtBlacklistService implements JwtBlacklistService {

    private final ReactiveStringRedisTemplate redisTemplate;

    private static final String PREFIX = "jwt:blacklist:";

    @Override
    public Mono<Boolean> isBlocked(String jti) {
        return redisTemplate
                .hasKey(PREFIX + jti)
                .onErrorResume(
                        e -> {
                            log.error(
                                    "Redis connection failed in RedisJwtBlacklistService for JTI {}. Allowing JWT to proceed.",
                                    jti,
                                    e);
                            return Mono.just(false);
                        })
                .defaultIfEmpty(false);
    }
}
