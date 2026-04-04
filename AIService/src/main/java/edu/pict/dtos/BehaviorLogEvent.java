package edu.pict.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BehaviorLogEvent {
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

