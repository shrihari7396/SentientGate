package edu.pict.mcpservice.service;

import edu.pict.mcpservice.feienClients.AiServiceFeignClient;
import edu.pict.mcpservice.model.AnomalyDetectionRequest;
import edu.pict.mcpservice.model.AnomalyDetectionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIClient {

    private final AiServiceFeignClient aiServiceFeignClient;

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
