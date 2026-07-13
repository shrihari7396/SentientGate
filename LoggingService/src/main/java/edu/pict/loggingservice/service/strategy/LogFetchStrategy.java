package edu.pict.loggingservice.service.strategy;

import edu.pict.loggingservice.entity.GatewayLogEntity;
import java.time.Instant;
import java.util.List;

/**
 * Strategy interface for fetching log events. Implementations provide different data sources
 * (Redis cache, PostgreSQL) and the resolver chains them: Redis first for speed, DB as fallback.
 */
public interface LogFetchStrategy {

    /**
     * Fetch log events for a given visitor UUID that occurred after the specified instant.
     * Maps directly to the gRPC {@code UserLogEventsRequest} parameters:
     * <ul>
     *   <li>{@code uuid} — the visitor's unique identifier (gRPC field: uuid)</li>
     *   <li>{@code since} — computed from gRPC field: {@code Instant.now() - duration minutes}</li>
     * </ul>
     *
     * @param uuid  the visitor UUID to query logs for
     * @param since the earliest timestamp to include
     * @return list of matching log entities, or empty list if no data found
     */
    List<GatewayLogEntity> fetchLogs(String uuid, Instant since);

    /** Strategy name for logging and debugging. */
    String strategyName();
}
