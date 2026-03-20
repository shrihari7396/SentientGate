package edu.pict.service;

import edu.pict.dtos.AnomalyDetectionRequest;
import edu.pict.dtos.AnomalyDetectionResponse;
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

        // Convert features → model prompt OR vector
        String prompt = buildPrompt(req);

        double score = ollamaService.predictAnomalyScore(prompt);

        boolean anomaly = score > 0.7;

        return AnomalyDetectionResponse.builder()
                .anomaly(anomaly)
                .confidence(score)
                .modelVersion("v1.0")
                .inferenceTimeMs(System.currentTimeMillis() - start)
                .build();
    }

    private String buildPrompt(AnomalyDetectionRequest req) {
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
                        req.getFailureRate(),
                        req.getRequestsPerMinute(),
                        req.getUniqueRoutesAccessed(),
                        req.getJwtReuseCount(),
                        req.getIpReputationScore(),
                        req.getRouteSensitivity());
    }
}
