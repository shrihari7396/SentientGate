package edu.pict.loggingservice.controller;

import edu.pict.loggingservice.dto.RouteStats;
import edu.pict.loggingservice.service.RouteAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/logs/routes")
@RequiredArgsConstructor
public class RouteAggregationController {

    private final RouteAggregationService routeAggregationService;

    @GetMapping("/summary")
    public List<RouteStats> summary(
            @RequestParam Instant start,
            @RequestParam Instant end
    ) {
        return routeAggregationService.summarize(start, end);
    }
}

