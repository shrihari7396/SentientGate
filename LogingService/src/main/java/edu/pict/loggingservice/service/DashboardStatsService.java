package edu.pict.loggingservice.service;

import edu.pict.loggingservice.dto.DashboardRawStats;
import edu.pict.loggingservice.dto.DashboardSummaryStats;
import edu.pict.loggingservice.dto.TimeBucketStats;
import edu.pict.loggingservice.repository.GatewayLogRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class DashboardStatsService {

    private final GatewayLogRepository gatewayLogRepository;

    @org.springframework.cache.annotation.Cacheable(value = "dashboardSummary", key = "#start.toString() + '-' + #end.toString()")
    public DashboardSummaryStats getSummary(Instant start, Instant end) {
        try {
            log.info("📊 Fetching dashboard summary from {} to {}", start, end);
            DashboardRawStats raw = gatewayLogRepository.summarizeDashboard(start, end);

            if (raw == null || raw.getTotalCount() == null || raw.getTotalCount() == 0) {
                log.info("ℹ️ No logs found for period, returning zeroed stats.");
                return new DashboardSummaryStats(0, 0, 0.0, 0);
            }

            long totalCount = raw.getTotalCount();
            long securityBlocks = raw.getSecurityBlocks() != null ? raw.getSecurityBlocks() : 0;

            Double p99 = null;
            try {
                p99 = gatewayLogRepository.aggregateP99Latency(start, end);
            } catch (Exception e) {
                log.warn("⚠️ P99 aggregation failed: {}", e.getMessage());
            }

            long durationSeconds = Duration.between(start, end).getSeconds();
            if (durationSeconds <= 0) durationSeconds = 1;
            long throughput = totalCount / durationSeconds;

            return new DashboardSummaryStats(
                    throughput, securityBlocks, p99 != null ? p99 : 0.0, totalCount);
        } catch (Exception e) {
            log.error("❌ Critical error in dashboard summary: {}", e.getMessage(), e);
            return new DashboardSummaryStats(0, 0, 0.0, 0); // Safe fallback
        }
    }

    @org.springframework.cache.annotation.Cacheable(value = "dashboardVelocity", key = "#start.toString() + '-' + #end.toString()")
    public List<TimeBucketStats> getVelocity(Instant start, Instant end) {
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
