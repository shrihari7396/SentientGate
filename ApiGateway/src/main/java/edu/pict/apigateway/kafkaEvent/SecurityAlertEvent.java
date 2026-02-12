package edu.pict.apigateway.kafkaEvent;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SecurityAlertEvent {
    private String uuid;
    private int errorCode; // e.g., 401, 429
    private String reason; // e.g., "RATE_LIMIT_EXCEEDED"
    private String attemptedPath;
    private String alertSeverity; // "LOW", "MEDIUM", "HIGH"
    private long timestamp;
}
