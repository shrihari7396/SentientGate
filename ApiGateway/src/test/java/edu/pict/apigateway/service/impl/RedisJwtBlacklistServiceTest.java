package edu.pict.apigateway.service.impl;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RedisJwtBlacklistServiceTest {

    @Mock private ReactiveStringRedisTemplate redisTemplate;

    @InjectMocks private RedisJwtBlacklistService blackListService;

    private static final String PREFIX = "jwt:blacklist:";

    @Test
    void testIsBlocked_True() {
        String jti = "test-jti-123";
        when(redisTemplate.hasKey(PREFIX + jti)).thenReturn(Mono.just(true));

        StepVerifier.create(blackListService.isBlocked(jti)).expectNext(true).verifyComplete();
    }

    @Test
    void testIsBlocked_False() {
        String jti = "non-existent-jti";
        when(redisTemplate.hasKey(PREFIX + jti)).thenReturn(Mono.just(false));

        StepVerifier.create(blackListService.isBlocked(jti)).expectNext(false).verifyComplete();
    }

    @Test
    void testIsBlocked_EmptyResponse() {
        String jti = "empty-jti";
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.empty());

        // The implementation uses .defaultIfEmpty(false)
        StepVerifier.create(blackListService.isBlocked(jti)).expectNext(false).verifyComplete();
    }
}
