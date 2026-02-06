package edu.pict.mcpservice.service;

import edu.pict.mcpservice.model.AIResult;
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

    private final RestTemplate restTemplate;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public AIResult analyze(Map<String, Object> features) {

        try {
            return restTemplate.postForObject(
                    aiServiceUrl + "/ai/anomaly/analyze",
                    features,
                    AIResult.class
            );
        } catch (Exception e) {
            log.error("AI service call failed", e);

            // FAIL-OPEN: no anomaly, zero confidence
            return new AIResult(false, 0.0, "unknown", 0);
        }
    }
}
