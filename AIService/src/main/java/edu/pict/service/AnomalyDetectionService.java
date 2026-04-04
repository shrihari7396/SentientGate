package edu.pict.service;

import edu.pict.dtos.AnomalyDetectionRequest;
import edu.pict.dtos.AnomalyDetectionResponse;
import edu.pict.dtos.BehaviorLogEvent;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionService {

    private final OllamaService ollamaService;

    public AnomalyDetectionResponse analyze(AnomalyDetectionRequest req) {

        long start = System.currentTimeMillis();
        FeatureVector features = FeatureVector.from(req);

        String prompt = buildPrompt(features);

        double score = ollamaService.predictAnomalyScore(prompt);

        boolean anomaly = score > 0.7;

        return AnomalyDetectionResponse.builder()
                .anomaly(anomaly)
                .confidence(score)
                .modelVersion("v1.0")
                .inferenceTimeMs(System.currentTimeMillis() - start)
                .build();
    }

    private String buildPrompt(FeatureVector vector) {
        return """
        You are an anomaly detection model.
        Given numeric behavior signals, return ONLY a number between 0 and 1.

        failureRate=%f
        requestsPerMinute=%d
        uniqueRoutes=%d
        jwtReuse=%d
        ipReputation=%f
        routeSensitivity=%s
        """
                .formatted(
                        vector.failureRate(),
                        vector.requestsPerMinute(),
                        vector.uniqueRoutesAccessed(),
                        vector.jwtReuseCount(),
                        vector.ipReputationScore(),
                        vector.routeSensitivity());
    }

    private record FeatureVector(
            double failureRate,
            int requestsPerMinute,
            int uniqueRoutesAccessed,
            int jwtReuseCount,
            double ipReputationScore,
            String routeSensitivity) {

        private static FeatureVector from(AnomalyDetectionRequest req) {
            List<BehaviorLogEvent> history = req.getHistory();
            if (history != null && !history.isEmpty()) {
                long errors = history.stream().filter(log -> log.getStatusCode() >= 400).count();
                long distinctRoutes =
                        history.stream().map(BehaviorLogEvent::getPath).filter(p -> p != null).distinct().count();
                String sensitivity =
                        history.stream()
                                        .map(BehaviorLogEvent::getPath)
                                        .filter(path -> path != null)
                                        .anyMatch(
                                                path ->
                                                        path.contains("/admin")
                                                                || path.contains("/internal")
                                                                || path.contains("/mcp"))
                                ? "HIGH"
                                : "MEDIUM";

                return new FeatureVector(
                        (double) errors / history.size(),
                        history.size(),
                        Math.toIntExact(distinctRoutes),
                        0,
                        0.5,
                        sanitizeSensitivity(sensitivity));
            }

            return new FeatureVector(
                    clamp(req.getFailureRate(), 0.0, 1.0),
                    Math.max(req.getRequestsPerMinute(), 0),
                    Math.max(req.getUniqueRoutesAccessed(), 0),
                    Math.max(req.getJwtReuseCount(), 0),
                    clamp(req.getIpReputationScore(), 0.0, 1.0),
                    sanitizeSensitivity(req.getRouteSensitivity()));
        }
    }

    private static String sanitizeSensitivity(String sensitivity) {
        if (sensitivity == null) {
            return "MEDIUM";
        }
        String normalized = sensitivity.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "LOW", "MEDIUM", "HIGH" -> normalized;
            default -> "MEDIUM";
        };
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
