package edu.pict.mcpservice.clients;

import edu.pict.mcpservice.model.AnomalyDetectionRequest;
import edu.pict.mcpservice.model.AnomalyDetectionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "AI-SERVICE")
public interface AiServiceFeignClient {

    @PostMapping("/ai-service/anomaly/analyze")
    ResponseEntity<AnomalyDetectionResponse> analyze(@RequestBody AnomalyDetectionRequest request);
}
