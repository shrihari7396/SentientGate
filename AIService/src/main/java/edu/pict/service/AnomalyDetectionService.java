package edu.pict.service;

import edu.pict.dtos.AnomalyDetectionRequest;
import edu.pict.dtos.AnomalyDetectionResponse;
import edu.pict.dtos.LogEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionService {

    private final OllamaService ollamaService;

    public Mono<AnomalyDetectionResponse> analyze(AnomalyDetectionRequest req) {
        long start = System.currentTimeMillis();

        List<LogEvent> history = req.getHistory();
        if (history == null) {
            history = List.of();
        }

        int totalRequests = history.size();

        // Calculate failureRate: count of logs with status code >= 400
        double failureRate = 0.0;
        long failureCount = history.stream()
                .filter(log -> log.getStatusCode() >= 400)
                .count();
        if (totalRequests > 0) {
            failureRate = (double) failureCount / totalRequests;
        }

        // Calculate requestsPerMinute
        double requestsPerMinute = 0.0;
        if (totalRequests > 0) {
            long minTimestamp = history.stream().mapToLong(LogEvent::getTimestamp).min().orElse(0L);
            long maxTimestamp = history.stream().mapToLong(LogEvent::getTimestamp).max().orElse(0L);
            double timeSpanMinutes = (maxTimestamp - minTimestamp) / 60000.0;
            if (timeSpanMinutes < 1.0) {
                timeSpanMinutes = 1.0;
            }
            requestsPerMinute = totalRequests / timeSpanMinutes;
        }

        // Calculate uniqueRoutesAccessed
        long uniqueRoutesAccessed = history.stream()
                .map(LogEvent::getPath)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

        // Calculate jwtReuseCount (using client IP switches as heuristic)
        long uniqueIps = history.stream()
                .map(LogEvent::getClientIp)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
        long jwtReuseCount = Math.max(0, uniqueIps - 1);

        // Calculate ipReputationScore
        double ipReputationScore = 1.0;
        if (uniqueIps > 1) {
            ipReputationScore = Math.max(0.1, 1.0 - (uniqueIps * 0.1));
        }

        // Determine routeSensitivity based on paths
        String routeSensitivity = "LOW";
        boolean hasHighSensitivity = history.stream()
                .map(LogEvent::getPath)
                .filter(java.util.Objects::nonNull)
                .anyMatch(path -> path.contains("/admin") || path.contains("/mgmt") || path.contains("/db") || path.contains("/actuator") || path.contains("/security"));
        boolean hasMediumSensitivity = history.stream()
                .map(LogEvent::getPath)
                .filter(java.util.Objects::nonNull)
                .anyMatch(path -> path.contains("/auth") || path.contains("/login") || path.contains("/private"));
        if (hasHighSensitivity) {
            routeSensitivity = "HIGH";
        } else if (hasMediumSensitivity) {
            routeSensitivity = "MEDIUM";
        }

        // Convert features -> model prompt
        String prompt = buildPrompt(failureRate, requestsPerMinute, uniqueRoutesAccessed, jwtReuseCount, ipReputationScore, routeSensitivity);

        return ollamaService.predictAnomalyScore(prompt).map(score -> {
            boolean anomaly = score > 0.7;

            return AnomalyDetectionResponse.builder()
                    .anomaly(anomaly)
                    .confidence(score)
                    .modelVersion("v1.0")
                    .inferenceTimeMs(System.currentTimeMillis() - start)
                    .isAnomaly(anomaly)
                    .confidenceScore(score)
                    .patternDetected(anomaly ? "AI_BEHAVIORAL_ANOMALY" : "NORMAL")
                    .suggestedBlockMinutes(anomaly ? 60 : 0)
                    .build();
        });
    }

    private String buildPrompt(double failureRate, double requestsPerMinute, long uniqueRoutes, long jwtReuse, double ipReputation, String routeSensitivity) {
        return """
        You are an anomaly detection model.
        Given numeric behavior signals, return ONLY a number between 0 and 1.

        failureRate=%f
        requestsPerMinute=%f
        uniqueRoutes=%d
        jwtReuse=%d
        ipReputation=%f
        routeSensitivity=%s
        """
                .formatted(
                        failureRate,
                        requestsPerMinute,
                        uniqueRoutes,
                        jwtReuse,
                        ipReputation,
                        routeSensitivity);
    }
}
