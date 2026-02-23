package edu.pict.loggingservice.controller;

import edu.pict.loggingservice.dto.DashboardSummaryStats;
import edu.pict.loggingservice.dto.TimeBucketStats;
import edu.pict.loggingservice.service.DashboardStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/logs/stats")
@RequiredArgsConstructor
public class DashboardStatsController {

    private final DashboardStatsService dashboardStatsService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryStats> getSummary(
            @RequestParam Instant start,
            @RequestParam Instant end) {
        return ResponseEntity.ok(dashboardStatsService.getSummary(start, end));
    }

    @GetMapping("/velocity")
    public ResponseEntity<List<TimeBucketStats>> getVelocity(
            @RequestParam Instant start,
            @RequestParam Instant end) {
        return ResponseEntity.ok(dashboardStatsService.getVelocity(start, end));
    }
}
