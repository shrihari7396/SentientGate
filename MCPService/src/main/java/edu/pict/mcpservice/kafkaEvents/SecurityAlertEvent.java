package edu.pict.mcpservice.kafkaEvents;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SecurityAlertEvent {
    private String uuid;
    private int errorCode;
    private String reason;
    private String attemptedPath;
    private String method;
    private String userAgent;
    private String clientIp;
    private String alertSeverity;
    private long timestamp;
}
