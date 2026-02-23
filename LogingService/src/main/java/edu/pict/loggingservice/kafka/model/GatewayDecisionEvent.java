package edu.pict.loggingservice.kafka.model;

public record GatewayDecisionEvent(
                String uuid,
                String path,
                String method,
                String clientIp,
                String routeId,
                String decision,
                int statusCode,
                long requestSize,
                int latencyMs,
                String queryParams,
                String userAgent,
                long timestamp) {
}
