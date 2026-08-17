package edu.pict.controller;

import edu.pict.dtos.AnomalyDetectionRequest;
import edu.pict.dtos.AnomalyDetectionResponse;
import edu.pict.service.AnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyDetectionService anomalyService;

    @PostMapping("/analyze")
    public Mono<ResponseEntity<AnomalyDetectionResponse>> analyze(
            @RequestBody AnomalyDetectionRequest request) {

        return anomalyService.analyze(request).map(ResponseEntity::ok);
    }
}
