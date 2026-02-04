package edu.pict.loggingservice.dto;

import java.time.Instant;

public record TimeBucketStats(
        Instant minute,
        long requestCount,
        long errorCount,
        long rateLimitedCount
) {}