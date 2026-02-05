package edu.pict.dtos;

import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyDetectionRequest {

    private double failureRate;
    private int requestsPerMinute;
    private int uniqueRoutesAccessed;
    private int jwtReuseCount;
    private double ipReputationScore;   // 0–1
    private String routeSensitivity;    // LOW | MEDIUM | HIGH
}
