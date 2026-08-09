package edu.pict.loggingservice.repository;

import edu.pict.loggingservice.dto.DashboardRawStats;
import edu.pict.loggingservice.entity.GatewayLogEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GatewayLogRepository extends JpaRepository<GatewayLogEntity, UUID> {

    @Query(
            """
        SELECT l FROM GatewayLogEntity l
        WHERE (:path IS NULL OR :path = '' OR l.path LIKE %:path%)
          AND (:uuid IS NULL OR :uuid = '' OR l.visitorId = :uuid)
          AND (
               :statusType IS NULL OR :statusType = ''
            OR (:statusType = '2xx' AND l.statusCode >= 200 AND l.statusCode < 300)
            OR (:statusType = '4xx' AND l.statusCode >= 400 AND l.statusCode < 500)
            OR (:statusType = '5xx' AND l.statusCode >= 500)
            OR (:statusType = 'exact' AND l.statusCode = :statusCodeVal)
          )
    """)
    Page<GatewayLogEntity> findWithFilters(
            String path, String uuid, String statusType, Integer statusCodeVal, Pageable pageable);

    List<GatewayLogEntity> findByClientIp(String clientIp);

    List<GatewayLogEntity> findByOccurredAtBetween(Instant start, Instant end);

    List<GatewayLogEntity> findByDecision(String decision);

    List<GatewayLogEntity> findByVisitorIdAndOccurredAtAfter(String visitorId, Instant occurredAt);

    @Query(
            """
                SELECT new edu.pict.loggingservice.dto.DashboardRawStats(
                    COUNT(l),
                    SUM(CASE WHEN l.decision = 'BLOCKED' OR l.statusCode >= 400 THEN 1L ELSE 0L END),
                    AVG(l.latencyMs * 1.0),
                    COUNT(DISTINCT l.clientIp)
                )
                FROM GatewayLogEntity l
                WHERE l.occurredAt BETWEEN :start AND :end
            """)
    DashboardRawStats summarizeDashboard(Instant start, Instant end);

    @Query(
            value =
                    "SELECT percentile_cont(0.99) WITHIN GROUP (ORDER BY latency_ms) FROM gateway_logs WHERE occurred_at BETWEEN :start AND :end",
            nativeQuery = true)
    Double aggregateP99Latency(Instant start, Instant end);

    // IP
    @Query(
            """
                SELECT
                    COUNT(l),
                    SUM(CASE WHEN l.decision = 'RATE_LIMITED' THEN 1 ELSE 0 END),
                    SUM(CASE WHEN l.decision = 'INVALID_JWT' THEN 1 ELSE 0 END),
                    COUNT(DISTINCT l.routeId),
                    AVG(l.latencyMs)
                FROM GatewayLogEntity l
                WHERE l.clientIp = :ip
                  AND l.occurredAt BETWEEN :start AND :end
            """)
    Object[] aggregateByIp(String ip, Instant start, Instant end);

    // TimeBucket
    @Query(
            """
                SELECT
                    FUNCTION('date_trunc', 'minute', l.occurredAt),
                    COUNT(l),
                    SUM(CASE WHEN l.statusCode >= 400 THEN 1 ELSE 0 END),
                    SUM(CASE WHEN l.decision = 'RATE_LIMITED' THEN 1 ELSE 0 END)
                FROM GatewayLogEntity l
                WHERE l.occurredAt BETWEEN :start AND :end
                GROUP BY FUNCTION('date_trunc', 'minute', l.occurredAt)
                ORDER BY FUNCTION('date_trunc', 'minute', l.occurredAt)
            """)
    List<Object[]> aggregateByMinute(Instant start, Instant end);

    // Route Level Routing
    @Query(
            """
                SELECT
                    l.routeId,
                    COUNT(l),
                    SUM(CASE WHEN l.statusCode >= 400 THEN 1 ELSE 0 END),
                    AVG(l.latencyMs)
                FROM GatewayLogEntity l
                WHERE l.occurredAt BETWEEN :start AND :end
                GROUP BY l.routeId
            """)
    List<Object[]> aggregateByRoute(Instant start, Instant end);
}
