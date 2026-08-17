package edu.pict.mcpservice.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyDetectionResponse {
    @JsonAlias("anomaly")
    private boolean isAnomaly;
    @JsonAlias("confidence")
    private double confidenceScore; // 0.0 to 1.0
    @JsonAlias("modelVersion")
    private String patternDetected; // e.g., "SEQUENTIAL_SCAN"
    @JsonAlias("inferenceTimeMs")
    private int suggestedBlockMinutes;
}
