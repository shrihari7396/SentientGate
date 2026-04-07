package edu.pict.loggingservice.controller;

import edu.pict.loggingservice.service.TimeWindowAggregationService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs/time")
@RequiredArgsConstructor
public class TimeAggregationController {

    private final TimeWindowAggregationService timeWindowAggregationService;

    @GetMapping("/summary")
    public ResponseEntity<?> summary(@RequestParam Instant start, @RequestParam Instant end) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(timeWindowAggregationService.summarize(start, end));
    }
}
