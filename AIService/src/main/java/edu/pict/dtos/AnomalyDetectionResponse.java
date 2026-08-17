package edu.pict.dtos;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnomalyDetectionResponse {

    private boolean anomaly;
    private double confidence;
    private String modelVersion;
    private long inferenceTimeMs;

    private boolean isAnomaly;
    private double confidenceScore;
    private String patternDetected;
    private int suggestedBlockMinutes;
}
