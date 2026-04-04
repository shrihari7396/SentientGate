package edu.pict.mcpservice.ports;

import edu.pict.mcpservice.model.AnomalyDetectionRequest;
import edu.pict.mcpservice.model.AnomalyDetectionResponse;

public interface AnomalyScoringPort {
    AnomalyDetectionResponse analyze(AnomalyDetectionRequest request);
}

