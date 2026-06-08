package edu.pict.mcpservice.clients;

import edu.pict.mcpservice.model.AnomalyDetectionRequest;
import edu.pict.mcpservice.model.AnomalyDetectionResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-inference-service")
public interface AiServiceFeignClient {

    @PostMapping("/api/v1/analyze")
    @CircuitBreaker(name = "ai-service-circuit-breaker", fallbackMethod = "analyzeFallback")
    ResponseEntity<AnomalyDetectionResponse> analyze(@RequestBody AnomalyDetectionRequest request);

    /**
     * Fallback method when circuit breaker is open or service is unavailable.
     * Returns a default response indicating the service is temporarily unavailable.
     */
    default ResponseEntity<AnomalyDetectionResponse> analyzeFallback(AnomalyDetectionRequest request, Exception ex) {
        AnomalyDetectionResponse fallbackResponse = AnomalyDetectionResponse.builder()
                .isAnomaly(false)
                .confidenceScore(0.0)
                .patternDetected("SERVICE_UNAVAILABLE")
                .suggestedBlockMinutes(0)
                .build();

        return ResponseEntity.ok(fallbackResponse);
    }
}
