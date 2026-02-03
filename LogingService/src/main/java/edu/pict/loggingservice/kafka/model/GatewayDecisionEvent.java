package edu.pict.loggingservice.kafka.model;

import java.util.UUID;

public record GatewayDecisionEvent(
        UUID eventId,
        String clientIp,
        String routeId,
        String decision,
        int statusCode,
        int latencyMs,
        long timestamp
) {}