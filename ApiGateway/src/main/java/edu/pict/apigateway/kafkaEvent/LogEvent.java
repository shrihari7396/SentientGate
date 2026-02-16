package edu.pict.apigateway.kafkaEvent;

import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;

@Builder
@Data
public class LogEvent {
    private String uuid;
    private String path;
    private String method;
    private long latencyMs;
    private String queryParams;
    private String clientIp;
    private int statusCode;
    private long requestSize;
    private long timestamp;
    private String userAgent;
    BigInteger
}
