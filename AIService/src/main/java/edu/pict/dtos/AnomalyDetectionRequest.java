package edu.pict.dtos;

import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyDetectionRequest {

    private String uuid;
    private List<BehaviorLogEvent> history;

    private double failureRate;
    private int requestsPerMinute;
    private int uniqueRoutesAccessed;
    private int jwtReuseCount;
    private double ipReputationScore; // 0–1
    private String routeSensitivity; // LOW | MEDIUM | HIGH
}
