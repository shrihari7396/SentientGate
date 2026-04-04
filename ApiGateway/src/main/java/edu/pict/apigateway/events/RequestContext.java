package edu.pict.apigateway.events;

public record RequestContext(
        String uuid,
        String path,
        String method,
        String routeId,
        String queryParams,
        long requestSize,
        String clientIp,
        String userAgent,
        long timestamp) {}

