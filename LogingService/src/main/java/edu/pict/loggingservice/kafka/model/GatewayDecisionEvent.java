package edu.pict.loggingservice.kafka.model;

public record GatewayDecisionEvent(
        String clientIp,
        String routeId,
        String decision,
        int statusCode,
        int latencyMs,
        long timestamp
) {}


