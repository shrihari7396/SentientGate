package edu.pict.mcpservice.service;

import edu.pict.mcpservice.grpc.UserLogEvent;
import edu.pict.mcpservice.grpc.UserLogEventResponse;
import edu.pict.mcpservice.grpc.UserLogEventServiceGrpc;
import edu.pict.mcpservice.grpc.UserLogEventsRequest;
import edu.pict.mcpservice.ports.HistoryProvider;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EventHistoryService implements HistoryProvider {

    @GrpcClient("logging-service")
    private UserLogEventServiceGrpc.UserLogEventServiceBlockingStub stub;
    private final Cache<String, List<UserLogEvent>> historyCache =
            Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(30)).maximumSize(10_000).build();

    /**
     * Fetches user history from the Logging Service via gRPC.
     *
     * @param uuid The unique visitor ID.
     * @param duration Lookback period in minutes.
     * @return List of Log Events or an empty list if service is down.
     */
    @Override
    public List<UserLogEvent> getAllEventsInDuration(String uuid, int duration) {
        String cacheKey = uuid + ":" + duration;
        return historyCache.get(cacheKey, key -> fetchFromGrpc(uuid, duration));
    }

    private List<UserLogEvent> fetchFromGrpc(String uuid, int duration) {
        try {
            log.info("📡 Requesting {} min history for UUID: {}", duration, uuid);

            UserLogEventsRequest request =
                    UserLogEventsRequest.newBuilder().setUuid(uuid).setDuration(duration).build();

            // Calling the remote Logging Service
            UserLogEventResponse response =
                    stub.withDeadlineAfter(2, TimeUnit.SECONDS).getUserEvents(request);

            log.info(
                    "✅ Successfully fetched {} events for UUID: {}",
                    response.getUserLogEventsCount(),
                    uuid);

            return response.getUserLogEventsList();

        } catch (StatusRuntimeException e) {
            // Agar Logging Service down hai ya network issue hai
            log.error("❌ gRPC call failed for UUID: {}. Error: {}", uuid, e.getStatus());

            // Return empty list to avoid NullPointerException in Strategies
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("❌ Unexpected error fetching history for UUID: {}", uuid, e);
            return new ArrayList<>();
        }
    }
}
