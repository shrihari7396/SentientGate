package edu.pict.apigateway.kafkaEvent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
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
