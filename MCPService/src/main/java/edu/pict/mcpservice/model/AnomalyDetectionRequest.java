package edu.pict.mcpservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyDetectionRequest {

    private double failureRate;
    private int requestsPerMinute;
    private int uniqueRoutesAccessed;
    private int jwtReuseCount;
    private double ipReputationScore;   // 0–1
    private String routeSensitivity;    // LOW | MEDIUM | HIGH
}
