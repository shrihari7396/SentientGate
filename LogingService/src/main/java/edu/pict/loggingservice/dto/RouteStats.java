package edu.pict.loggingservice.dto;

public record RouteStats(
        String routeId,
        long requestCount,
        double errorRate,
        double avgLatencyMs
) {}