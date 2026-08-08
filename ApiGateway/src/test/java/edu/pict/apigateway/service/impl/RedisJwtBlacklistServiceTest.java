package edu.pict.apigateway.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RedisJwtBlacklistServiceTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @InjectMocks
    private RedisJwtBlacklistService redisJwtBlacklistService;

    private static final String PREFIX = "jwt:blacklist:";

    @Test
    void testIsBlocked_TokenIsBlacklisted() {
        String jti = "test-jti-blocked";
        when(redisTemplate.hasKey(PREFIX + jti)).thenReturn(Mono.just(true));

        StepVerifier.create(redisJwtBlacklistService.isBlocked(jti))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void testIsBlocked_TokenIsNotBlacklisted() {
        String jti = "test-jti-allowed";
        when(redisTemplate.hasKey(PREFIX + jti)).thenReturn(Mono.just(false));

        StepVerifier.create(redisJwtBlacklistService.isBlocked(jti))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void testIsBlocked_EmptyResponse() {
        String jti = "test-jti-empty";
        when(redisTemplate.hasKey(PREFIX + jti)).thenReturn(Mono.empty());

        StepVerifier.create(redisJwtBlacklistService.isBlocked(jti))
                .expectNext(false) // defaultIfEmpty(false)
                .verifyComplete();
    }

    @Test
    void testIsBlocked_RedisExceptionFailsOpen() {
        String jti = "test-jti-error";
        when(redisTemplate.hasKey(PREFIX + jti)).thenReturn(Mono.error(new RuntimeException("Redis connection error")));

        StepVerifier.create(redisJwtBlacklistService.isBlocked(jti))
                .expectNext(false) // onErrorResume(false)
                .verifyComplete();
    }
}
