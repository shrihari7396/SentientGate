package edu.pict.loggingservice.service.strategy;

import edu.pict.loggingservice.entity.GatewayLogEntity;
import edu.pict.loggingservice.repository.GatewayLogRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback strategy that fetches log events from PostgreSQL. Used when the Redis cache misses (key
 * expired or data not yet cached). This is the authoritative data source for long-term stored logs.
 *
 * <p>Supports gRPC query parameters:
 *
 * <ul>
 *   <li><b>uuid</b> — maps to {@code visitorId} column
 *   <li><b>duration</b> (via {@code since}) — maps to {@code occurredAt > since} filter
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseFetchStrategy implements LogFetchStrategy {

    private final GatewayLogRepository gatewayLogRepository;

    @Override
    public List<GatewayLogEntity> fetchLogs(String uuid, Instant since) {
        List<GatewayLogEntity> logs =
                gatewayLogRepository.findByVisitorIdAndOccurredAtAfter(uuid, since);
        log.debug("Database fetch for uuid={}: {} entries (since={})", uuid, logs.size(), since);
        return logs;
    }

    @Override
    public String strategyName() {
        return "DATABASE";
    }
}
