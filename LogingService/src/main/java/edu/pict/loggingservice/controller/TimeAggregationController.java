package edu.pict.loggingservice.controller;

import edu.pict.loggingservice.dto.TimeBucketStats;
import edu.pict.loggingservice.service.TimeWindowAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/logs/time")
@RequiredArgsConstructor
public class TimeAggregationController {

    private final TimeWindowAggregationService timeWindowAggregationService;

    @GetMapping("/summary")
    public List<TimeBucketStats> summary(
            @RequestParam Instant start,
            @RequestParam Instant end
    ) {
        return timeWindowAggregationService.summarize(start, end);
    }
}

