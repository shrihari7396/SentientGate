package edu.pict.mcpservice.service;

import edu.pict.mcpservice.grpc.UserLogEvent;
import edu.pict.mcpservice.grpc.UserLogEventResponse;
import edu.pict.mcpservice.grpc.UserLogEventServiceGrpc;
import edu.pict.mcpservice.grpc.UserLogEventsRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EventHistoryService {

    private static final String HISTORY_CACHE_PREFIX = "mcp:history:";
    private static final Duration HISTORY_CACHE_TTL = Duration.ofSeconds(30);

    @GrpcClient("logging-service")
    private UserLogEventServiceGrpc.UserLogEventServiceBlockingStub stub;
    private final StringRedisTemplate stringRedisTemplate;

    public EventHistoryService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }


    public List<UserLogEvent> getAllEventsInDuration(String uuid, int duration) {
        String cacheKey = new StringBuilder().append(HISTORY_CACHE_PREFIX)
                .append(uuid)
                .append(":")
                .append(duration)
                .toString();

        String cachedPayload = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedPayload != null) {
            try {
                byte[] bytes = Base64.getDecoder().decode(cachedPayload);
                UserLogEventResponse cachedResponse = UserLogEventResponse.parseFrom(bytes);
                return cachedResponse.getUserLogEventsList();
            } catch (Exception e) {
                log.warn("Failed to deserialize cached history for key {}", cacheKey, e);
            }
        }

        try {
            // Add a small delay for event sourcing race-condition resolution (Flaw 6)
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<UserLogEvent> fetched = fetchFromGrpc(uuid, duration);
        try {

            UserLogEventResponse response =
                    UserLogEventResponse.newBuilder()
                            .addAllUserLogEvents(fetched)
                            .build();

            String encodedPayload = Base64.getEncoder().encodeToString(response.toByteArray());
            stringRedisTemplate.opsForValue().set(cacheKey, encodedPayload, HISTORY_CACHE_TTL);

        } catch (Exception e) {
            log.warn("Failed to cache history for key {}", cacheKey, e);
        }

        return fetched;
    }

    @CircuitBreaker(name = "loggingService", fallbackMethod = "fallbackHistory")
    public List<UserLogEvent> fetchFromGrpc(String uuid, int duration) {
        log.info("📡 Requesting {} min history for UUID: {}", duration, uuid);

        UserLogEventsRequest request =
                UserLogEventsRequest.newBuilder().setUuid(uuid).setDuration(duration).build();

        // Calling the remote Logging Service with 2s Deadline (Flaw 9)
        UserLogEventResponse response = stub.getUserEvents(request);
        log.info(
                "✅ Successfully fetched {} events for UUID: {}",
                response.getUserLogEventsCount(),
                uuid);

        return response.getUserLogEventsList();
    }

    public List<UserLogEvent> fallbackHistory(String uuid, int duration, Throwable t) {
        log.warn(
                "❌ gRPC call failed or Circuit open for UUID: {}. Error: {}", uuid, t.getMessage());
        return new ArrayList<>();
    }
}
