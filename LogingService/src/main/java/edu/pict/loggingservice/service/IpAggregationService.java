package edu.pict.loggingservice.service;

import edu.pict.loggingservice.dto.IpActivitySummary;
import edu.pict.loggingservice.repository.GatewayLogRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class IpAggregationService {

    private final GatewayLogRepository gatewayLogRepository;

    public IpActivitySummary summarize(String ip, Instant start, Instant end) {
        Object[] r = gatewayLogRepository.aggregateByIp(ip, start, end);

        return new IpActivitySummary(
                ip,
                ((Number) r[0]).longValue(),
                ((Number) r[1]).longValue(),
                ((Number) r[2]).longValue(),
                ((Number) r[3]).longValue(),
                r[4] != null ? ((Number) r[4]).doubleValue() : 0.0);
    }
}
