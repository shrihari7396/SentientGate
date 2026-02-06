package edu.pict.mcpservice.feienClients;

import edu.pict.mcpservice.model.AnomalyDetectionRequest;
import edu.pict.mcpservice.model.AnomalyDetectionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-service")
public interface AiServiceFeignClient {
    @PostMapping("/api/aiService/anomaly/analyze")
    ResponseEntity<AnomalyDetectionResponse> analyze(@RequestBody AnomalyDetectionRequest request);
}
