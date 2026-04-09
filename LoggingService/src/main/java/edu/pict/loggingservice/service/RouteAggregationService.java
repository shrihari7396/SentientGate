package edu.pict.loggingservice.service;

import edu.pict.loggingservice.dto.RouteStats;
import edu.pict.loggingservice.repository.GatewayLogRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RouteAggregationService {

    private final GatewayLogRepository gatewayLogRepository;

    public List<RouteStats> summarize(Instant start, Instant end) {
        return gatewayLogRepository.aggregateByRoute(start, end).stream()
                .map(
                        r -> {
                            long total = ((Number) r[1]).longValue();
                            long errors = ((Number) r[2]).longValue();

                            return new RouteStats(
                                    (String) r[0],
                                    total,
                                    total == 0 ? 0.0 : (errors * 1.0) / total,
                                    r[3] != null ? ((Number) r[3]).doubleValue() : 0.0);
                        })
                .toList();
    }
}
