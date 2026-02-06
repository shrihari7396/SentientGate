package edu.pict.mcpservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Decision {

    private DecisionType type;
    private String reason;
    private int riskScore;
    private double aiConfidence;
    private long ttlSeconds;
}
