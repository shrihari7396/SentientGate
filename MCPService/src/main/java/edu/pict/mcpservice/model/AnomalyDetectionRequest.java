package edu.pict.mcpservice.model;

import edu.pict.mcpservice.kafkaEvents.LogEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyDetectionRequest {
    private String uuid;
    private List<LogEvent> history;
}