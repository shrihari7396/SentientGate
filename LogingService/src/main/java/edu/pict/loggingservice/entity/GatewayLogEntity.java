package edu.pict.loggingservice.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
        name = "gateway_logs",
        indexes = {
            @Index(name = "idx_logs_occurred_at", columnList = "occurredAt"),
            @Index(name = "idx_logs_visitor_occurred", columnList = "visitorId,occurredAt"),
            @Index(name = "idx_logs_ip_occurred", columnList = "clientIp,occurredAt"),
            @Index(name = "idx_logs_status_occurred", columnList = "statusCode,occurredAt")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayLogEntity {

    @Id private UUID id;

    @Column(nullable = false)
    private String clientIp;

    @Column private String visitorId;

    @Column private String path;

    @Column private String method;

    @Column private String routeId;

    @Column private String decision;

    @Column(nullable = false)
    private int statusCode;

    @Column private long requestSize;

    @Column(nullable = false)
    private long latencyMs;

    @Column(columnDefinition = "TEXT")
    private String queryParams;

    @Column(columnDefinition = "TEXT")
    private String userAgent;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private Instant ingestedAt;

    @PrePersist
    public void prePersist() {
        this.ingestedAt = Instant.now();
    }
}
