package edu.pict.mcpservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import edu.pict.mcpservice.grpc.UserLogEvent;
import edu.pict.mcpservice.grpc.UserLogEventResponse;
import edu.pict.mcpservice.grpc.UserLogEventServiceGrpc;
import edu.pict.mcpservice.grpc.UserLogEventsRequest;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
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
class EventHistoryServiceTest {

    @Mock private UserLogEventServiceGrpc.UserLogEventServiceBlockingStub stub;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks private EventHistoryService eventHistoryService;

    private final String uuid = "user-123";
    private final int duration = 10;
    private final String cacheKey = "mcp:history:user-123:10";

    @BeforeEach
    void setup() {
        // Required for mock setup of StringRedisTemplate operations
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        org.springframework.test.util.ReflectionTestUtils.setField(
                eventHistoryService, "stub", stub);
    }

    @Nested
    @DisplayName("getAllEventsInDuration")
    class GetAllEvents {

        @Test
        @DisplayName("Returns cached events if available (skips gRPC)")
        void returnsCachedEvents() {
            // Setup a mock cached response
            UserLogEvent event = UserLogEvent.newBuilder().setUuid(uuid).setStatusCode(200).build();
            UserLogEventResponse response =
                    UserLogEventResponse.newBuilder().addUserLogEvents(event).build();
            String encodedCache = Base64.getEncoder().encodeToString(response.toByteArray());

            when(valueOperations.get(cacheKey)).thenReturn(encodedCache);

            List<UserLogEvent> result = eventHistoryService.getAllEventsInDuration(uuid, duration);

            assertEquals(1, result.size());
            assertEquals(uuid, result.get(0).getUuid());
            verifyNoInteractions(stub); // gRPC never called
        }

        @Test
        @DisplayName("Fetches from gRPC and caches result if not in cache")
        void fetchesFromGrpcAndCaches() {
            // Cache miss
            when(valueOperations.get(cacheKey)).thenReturn(null);

            // Mock gRPC response
            UserLogEvent event = UserLogEvent.newBuilder().setUuid(uuid).setStatusCode(404).build();
            UserLogEventResponse response =
                    UserLogEventResponse.newBuilder().addUserLogEvents(event).build();
            when(stub.getUserEvents(any(UserLogEventsRequest.class))).thenReturn(response);

            List<UserLogEvent> result = eventHistoryService.getAllEventsInDuration(uuid, duration);

            assertEquals(1, result.size());
            assertEquals(uuid, result.get(0).getUuid());

            // Verify gRPC called once
            verify(stub).getUserEvents(any(UserLogEventsRequest.class));

            // Verify cache update (encoded byte array)
            verify(valueOperations)
                    .set(eq(cacheKey), any(String.class), eq(Duration.ofSeconds(30)));
        }

        @Test
        @DisplayName("Handles corrupt cache data gracefully and falls back to gRPC")
        void handlesCorruptCacheData() {
            // Return corrupt base64 string
            when(valueOperations.get(cacheKey)).thenReturn("not-base-64");

            // Mock gRPC response
            UserLogEvent event = UserLogEvent.newBuilder().setUuid(uuid).setStatusCode(500).build();
            UserLogEventResponse response =
                    UserLogEventResponse.newBuilder().addUserLogEvents(event).build();
            when(stub.getUserEvents(any(UserLogEventsRequest.class))).thenReturn(response);

            List<UserLogEvent> result = eventHistoryService.getAllEventsInDuration(uuid, duration);

            assertEquals(1, result.size());
            assertEquals(500, result.get(0).getStatusCode());
            verify(stub).getUserEvents(any(UserLogEventsRequest.class));
        }
    }

    @Nested
    @DisplayName("fallbackHistory")
    class FallbackHistory {

        @Test
        @DisplayName("Returns empty list on circuit breaker fallback")
        void returnsEmptyListOnFallback() {
            List<UserLogEvent> result =
                    eventHistoryService.fallbackHistory(
                            uuid, duration, new RuntimeException("gRPC timeout"));

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
}
