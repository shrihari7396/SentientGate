package edu.pict.loggingservice.dto;

public record IpActivitySummary(
        String clientIp,
        long totalRequests,
        long rateLimitedCount,
        long invalidJwtCount,
        long uniqueRoutes,
        double avgLatencyMs
) {}