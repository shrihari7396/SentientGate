package edu.pict.loggingservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gateway_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayLogEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String clientIp;

    @Column(nullable = false)
    private String routeId;

    @Column(nullable = false)
    private String decision;

    @Column(nullable = false)
    private int statusCode;

    @Column(nullable = false)
    private int latencyMs;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private Instant ingestedAt;

    @PrePersist
    public void prePersist() {
        this.ingestedAt = Instant.now();
    }
}
