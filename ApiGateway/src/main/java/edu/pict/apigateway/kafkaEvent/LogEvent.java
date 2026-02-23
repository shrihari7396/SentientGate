package edu.pict.apigateway.kafkaEvent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {
    private String uuid;
    private String path;
    private String method;
    private String routeId;
    private String decision;
    private long latencyMs;
    private String queryParams;
    private String clientIp;
    private int statusCode;
    private long requestSize;
    private long timestamp;
    private String userAgent;
}
