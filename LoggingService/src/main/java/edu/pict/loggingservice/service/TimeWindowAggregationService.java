package edu.pict.loggingservice.service;

import edu.pict.loggingservice.dto.TimeBucketStats;
import edu.pict.loggingservice.repository.GatewayLogRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimeWindowAggregationService {

    private final GatewayLogRepository gatewayLogRepository;

    public List<TimeBucketStats> summarize(Instant start, Instant end) {
        return gatewayLogRepository.aggregateByMinute(start, end).stream()
                .map(
                        r ->
                                new TimeBucketStats(
                                        ((Instant) r[0]),
                                        ((Number) r[1]).longValue(),
                                        ((Number) r[2]).longValue(),
                                        ((Number) r[3]).longValue()))
                .toList();
    }
}
