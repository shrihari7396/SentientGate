package edu.pict.loggingservice.service.strategy;

import edu.pict.loggingservice.entity.GatewayLogEntity;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the Strategy Pattern for log fetching. Chains the strategies in priority order:
 *
 * <ol>
 *   <li><b>RedisFetchStrategy</b> — fast path (~1ms), checks the Redis cache first</li>
 *   <li><b>DatabaseFetchStrategy</b> — fallback, queries PostgreSQL if Redis returns empty</li>
 * </ol>
 *
 * <p>This resolver maps directly to the gRPC {@code GetUserEvents} call. The gRPC handler passes
 * in {@code uuid} and {@code since} (computed from the request's {@code duration} field), and
 * the resolver returns log entities from whichever strategy succeeds first.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogFetchStrategyResolver {

    private final RedisFetchStrategy redisFetchStrategy;
    private final DatabaseFetchStrategy databaseFetchStrategy;

    /**
     * Fetch logs using the strategy chain: Redis first, then DB fallback.
     *
     * @param uuid  the visitor UUID (gRPC request field)
     * @param since the earliest timestamp (computed from gRPC request's duration field)
     * @return list of log entities from the fastest available source
     */
    public List<GatewayLogEntity> fetchLogs(String uuid, Instant since) {

        // Strategy 1: Try Redis (fast path — resolves race condition)
        List<GatewayLogEntity> redisResult = redisFetchStrategy.fetchLogs(uuid, since);
        if (!redisResult.isEmpty()) {
            log.info(
                    "Resolved via {} strategy: {} logs for uuid={}",
                    redisFetchStrategy.strategyName(),
                    redisResult.size(),
                    uuid);
            return redisResult;
        }

        // Strategy 2: Fallback to Database (authoritative source)
        List<GatewayLogEntity> dbResult = databaseFetchStrategy.fetchLogs(uuid, since);
        log.info(
                "Resolved via {} strategy (Redis miss): {} logs for uuid={}",
                databaseFetchStrategy.strategyName(),
                dbResult.size(),
                uuid);
        return dbResult;
    }
}
