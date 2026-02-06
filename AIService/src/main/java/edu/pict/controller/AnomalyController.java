package edu.pict.controller;

import edu.pict.dtos.AnomalyDetectionRequest;
import edu.pict.dtos.AnomalyDetectionResponse;
import edu.pict.service.AnomalyDetectionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/anomaly")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyDetectionService anomalyService;

    @PostMapping("/analyze")
    public ResponseEntity<AnomalyDetectionResponse> analyze(
            @RequestBody AnomalyDetectionRequest request) {

        return ResponseEntity.ok(anomalyService.analyze(request));
    }
}
