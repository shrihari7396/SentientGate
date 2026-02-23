package edu.pict.loggingservice.repository;

import edu.pict.loggingservice.entity.GatewayLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface GatewayLogRepository extends JpaRepository<GatewayLogEntity, UUID> {

    @Query("SELECT l FROM GatewayLogEntity l WHERE (:path IS NULL OR l.path LIKE %:path%) AND (:statusCode IS NULL OR l.statusCode = :statusCode)")
    Page<GatewayLogEntity> findWithFilters(String path, Integer statusCode, Pageable pageable);

    List<GatewayLogEntity> findByClientIp(String clientIp);

    List<GatewayLogEntity> findByOccurredAtBetween(Instant start, Instant end);

    List<GatewayLogEntity> findByDecision(String decision);

    // IP
    @Query("""
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
    Object[] aggregateByIp(
            String ip,
            Instant start,
            Instant end);

    // TimeBucket
    @Query("""
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
    List<Object[]> aggregateByMinute(
            Instant start,
            Instant end);

    // Route Level Routing
    @Query("""
                SELECT
                    l.routeId,
                    COUNT(l),
                    SUM(CASE WHEN l.statusCode >= 400 THEN 1 ELSE 0 END),
                    AVG(l.latencyMs)
                FROM GatewayLogEntity l
                WHERE l.occurredAt BETWEEN :start AND :end
                GROUP BY l.routeId
            """)
    List<Object[]> aggregateByRoute(
            Instant start,
            Instant end);
}
