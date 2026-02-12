package edu.pict.apigateway.service.impl;

import edu.pict.apigateway.service.JwtBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RedisJwtBlacklistService implements JwtBlacklistService {

    private final ReactiveStringRedisTemplate redisTemplate;

    private static final String PREFIX = "jwt:blacklist:";

    @Override
    public Mono<Boolean> isBlocked(String jti) {
        return redisTemplate
                .hasKey(PREFIX + jti)
                .defaultIfEmpty(false);
    }
}
