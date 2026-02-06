package edu.pict.mcpservice.model;

import lombok.Data;
import java.time.Instant;

@Data
public class GatewayEvent {

    private String ip;
    private String jwtHash;
    private String route;
    private String method;

    private int statusCode;
    private long latencyMs;

    private Instant timestamp;
    private String serviceName;
}