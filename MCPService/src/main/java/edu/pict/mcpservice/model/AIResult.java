package edu.pict.mcpservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIResult {

    private boolean anomaly;
    private double confidence;
    private String modelVersion;
    private long inferenceTimeMs;
}