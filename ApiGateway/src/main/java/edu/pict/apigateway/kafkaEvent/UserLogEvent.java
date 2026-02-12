package edu.pict.apigateway.kafkaEvent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserLogEvent {
    private String uuid;
    private String path;
    private String method;
    private int statusCode;
    private long latencyMs;
    private long timestamp; // epoch milliseconds
    private String clientIp;
}
