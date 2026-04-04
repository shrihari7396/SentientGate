package edu.pict.mcpservice.service;

import edu.pict.mcpservice.clients.AiServiceFeignClient;
import edu.pict.mcpservice.model.AnomalyDetectionRequest;
import edu.pict.mcpservice.model.AnomalyDetectionResponse;
import edu.pict.mcpservice.ports.AnomalyScoringPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIClient implements AnomalyScoringPort {

    private final AiServiceFeignClient aiServiceFeignClient;

    @Override
    public AnomalyDetectionResponse analyze(AnomalyDetectionRequest anomalyDetectionRequest) {

        try {
            return aiServiceFeignClient.analyze(anomalyDetectionRequest).getBody();
        } catch (Exception e) {
            log.error("AI service call failed", e);
            // FAIL-OPEN: no anomaly, zero confidence
            return new AnomalyDetectionResponse(false, 0.0, "unknown", 0);
        }
    }
}
