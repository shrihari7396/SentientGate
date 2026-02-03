package edu.pict.loggingservice.repository;

import edu.pict.loggingservice.entity.GatewayLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface GatewayLogRepository extends JpaRepository<GatewayLogEntity, UUID> {

    List<GatewayLogEntity> findByClientIp(String clientIp);

    List<GatewayLogEntity> findByOccurredAtBetween(
            Instant start,
            Instant end
    );

    List<GatewayLogEntity> findByDecision(String decision);
}
