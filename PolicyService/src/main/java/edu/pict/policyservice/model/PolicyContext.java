package edu.pict.policyservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyContext {

    private int failedAuthCount;
    private int requestsPerMinute;
    private int jwtReuseCount;

    private String route;
    private String routeSensitivity; // LOW | MEDIUM | HIGH

    private boolean ipSeenBefore;
}
