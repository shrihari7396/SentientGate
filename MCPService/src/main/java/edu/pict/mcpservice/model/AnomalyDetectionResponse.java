package edu.pict.mcpservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyDetectionResponse {
    private boolean isAnomaly;
    private double confidenceScore; // 0.0 to 1.0
    private String patternDetected; // e.g., "SEQUENTIAL_SCAN"
    private int suggestedBlockMinutes;
}