package edu.pict.dtos;

import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyDetectionRequest {
    private String uuid;
    private List<LogEvent> history;
}
