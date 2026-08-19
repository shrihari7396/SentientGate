package edu.pict.loggingservice.dto;

import java.time.Instant;
import java.io.Serializable;

public record TimeBucketStats(
        Instant minute, long requestCount, long errorCount, long rateLimitedCount) implements Serializable {}
