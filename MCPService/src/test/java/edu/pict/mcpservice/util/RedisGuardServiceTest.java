package edu.pict.mcpservice.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisGuardServiceTest {

    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks private RedisGuardService redisGuardService;

    @Nested
    @DisplayName("wasRecentlyChecked")
    class WasRecentlyChecked {

        @Test
        @DisplayName("Returns true if key exists in Redis")
        void returnsTrueIfKeyExists() {
            when(stringRedisTemplate.hasKey("mcp:checked:user-123")).thenReturn(true);
            assertTrue(redisGuardService.wasRecentlyChecked("user-123"));
        }

        @Test
        @DisplayName("Returns false if key does not exist")
        void returnsFalseIfKeyDoesNotExist() {
            when(stringRedisTemplate.hasKey("mcp:checked:user-123")).thenReturn(false);
            assertFalse(redisGuardService.wasRecentlyChecked("user-123"));
        }

        @Test
        @DisplayName("Returns false if hasKey returns null")
        void returnsFalseIfNull() {
            when(stringRedisTemplate.hasKey("mcp:checked:user-123")).thenReturn(null);
            assertFalse(redisGuardService.wasRecentlyChecked("user-123"));
        }
    }

    @Nested
    @DisplayName("markAsChecked")
    class MarkAsChecked {

        @Test
        @DisplayName("Sets the checked key with the correct TTL")
        void setsKeyWithTtl() {
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

            redisGuardService.markAsChecked("user-123");

            verify(valueOperations)
                    .set(
                            eq("mcp:checked:user-123"),
                            any(String.class),
                            eq(Duration.ofSeconds(200)));
        }
    }

    @Nested
    @DisplayName("isFirstOccurrence")
    class IsFirstOccurrence {

        @BeforeEach
        void setup() {
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        }

        @Test
        @DisplayName("Returns true if setIfAbsent succeeds (first time)")
        void returnsTrueIfFirstTime() {
            when(valueOperations.setIfAbsent(
                            eq("mcp:dedup:user-123:429"),
                            any(String.class),
                            eq(Duration.ofSeconds(30))))
                    .thenReturn(true);

            assertTrue(redisGuardService.isFirstOccurrence("user-123", 429));
        }

        @Test
        @DisplayName("Returns false if setIfAbsent fails (duplicate)")
        void returnsFalseIfDuplicate() {
            when(valueOperations.setIfAbsent(
                            eq("mcp:dedup:user-123:429"),
                            any(String.class),
                            eq(Duration.ofSeconds(30))))
                    .thenReturn(false);

            assertFalse(redisGuardService.isFirstOccurrence("user-123", 429));
        }

        @Test
        @DisplayName("Returns false if setIfAbsent returns null")
        void returnsFalseIfNull() {
            when(valueOperations.setIfAbsent(
                            eq("mcp:dedup:user-123:429"),
                            any(String.class),
                            eq(Duration.ofSeconds(30))))
                    .thenReturn(null);

            assertFalse(redisGuardService.isFirstOccurrence("user-123", 429));
        }
    }
}
