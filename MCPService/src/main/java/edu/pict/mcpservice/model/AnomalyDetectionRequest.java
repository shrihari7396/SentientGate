package edu.pict.mcpservice.model;

import edu.pict.mcpservice.kafkaEvents.LogEvent;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyDetectionRequest {
    private String uuid;
    private List<LogEvent> history;
}
