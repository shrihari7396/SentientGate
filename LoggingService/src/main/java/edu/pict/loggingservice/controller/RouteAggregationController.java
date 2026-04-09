package edu.pict.loggingservice.controller;

import edu.pict.loggingservice.service.RouteAggregationService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs/routes")
@RequiredArgsConstructor
public class RouteAggregationController {

    private final RouteAggregationService routeAggregationService;

    @GetMapping("/summary")
    public ResponseEntity<?> summary(@RequestParam Instant start, @RequestParam Instant end) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(routeAggregationService.summarize(start, end));
    }
}
