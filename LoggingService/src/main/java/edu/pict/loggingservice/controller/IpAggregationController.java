package edu.pict.loggingservice.controller;

import edu.pict.loggingservice.service.IpAggregationService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class IpAggregationController {

    private final IpAggregationService ipAggregationService;

    @GetMapping("/{ip}/summary")
    public ResponseEntity<?> summary(
            @PathVariable String ip, @RequestParam Instant start, @RequestParam Instant end) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ipAggregationService.summarize(ip, start, end));
    }
}
