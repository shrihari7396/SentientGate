package edu.pict.apigateway.kafkaEvent;

public record GatewayDecisionEvent(
        String clientIp,
        String routeId,
        String decision,
        int statusCode,
        long latencyMs,
        long timestamp
) {}
