package edu.pict.mcpservice.service;

import edu.pict.mcpservice.grpc.UserLogEvent;
import edu.pict.mcpservice.grpc.UserLogEventResponse;
import edu.pict.mcpservice.grpc.UserLogEventServiceGrpc;
import edu.pict.mcpservice.grpc.UserLogEventsRequest;
import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Collections;

@Service
@Slf4j
public class EventHistoryService {

    @GrpcClient("logging-service")
    private UserLogEventServiceGrpc.UserLogEventServiceBlockingStub stub;

    // Cache user history for 30 seconds
    private final Cache<String, List<UserLogEvent>> historyCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .maximumSize(10_000)
            .build();

    /**
     * Fetches user history from the Logging Service via gRPC, utilizing caching.
     *
     * @param uuid The unique visitor ID.
     * @param duration Lookback period in minutes.
     * @return List of Log Events or an empty list if service is down.
     */
    public List<UserLogEvent> getAllEventsInDuration(String uuid, int duration) {
        return historyCache.get(uuid, key -> {
            try {
                // Add a small delay for event sourcing race-condition resolution (Flaw 6)
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return fetchFromGrpc(key, duration);
        });
    }

    @CircuitBreaker(name = "loggingService", fallbackMethod = "fallbackHistory")
    public List<UserLogEvent> fetchFromGrpc(String uuid, int duration) {
        log.info("📡 Requesting {} min history for UUID: {}", duration, uuid);

        UserLogEventsRequest request =
                UserLogEventsRequest.newBuilder().setUuid(uuid).setDuration(duration).build();

        // Calling the remote Logging Service with 2s Deadline (Flaw 9)
        UserLogEventResponse response = stub.withDeadlineAfter(2, TimeUnit.SECONDS).getUserEvents(request);

        log.info(
                "✅ Successfully fetched {} events for UUID: {}",
                response.getUserLogEventsCount(),
                uuid);

        return response.getUserLogEventsList();
    }

    public List<UserLogEvent> fallbackHistory(String uuid, int duration, Throwable t) {
        log.warn("❌ gRPC call failed or Circuit open for UUID: {}. Error: {}", uuid, t.getMessage());
        return new ArrayList<>();
    }
}
