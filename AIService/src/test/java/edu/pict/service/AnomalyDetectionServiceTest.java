package edu.pict.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import edu.pict.dtos.AnomalyDetectionRequest;
import edu.pict.dtos.LogEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    @Mock private OllamaService ollamaService;

    @InjectMocks private AnomalyDetectionService anomalyDetectionService;

    @BeforeEach
    void setUp() {
        // Default mock behavior
        when(ollamaService.predictAnomalyScore(anyString())).thenReturn(Mono.just(0.2));
    }

    @Test
    void testAnalyze_NullHistory() {
        AnomalyDetectionRequest request = new AnomalyDetectionRequest();
        request.setHistory(null);

        StepVerifier.create(anomalyDetectionService.analyze(request))
                .expectNextMatches(
                        response ->
                                !response.isAnomaly()
                                        && response.getConfidenceScore() == 0.2
                                        && "NORMAL".equals(response.getPatternDetected())
                                        && response.getSuggestedBlockMinutes() == 0)
                .verifyComplete();
    }

    @Test
    void testAnalyze_EmptyHistory() {
        AnomalyDetectionRequest request = new AnomalyDetectionRequest();
        request.setHistory(new ArrayList<>());

        StepVerifier.create(anomalyDetectionService.analyze(request))
                .expectNextMatches(
                        response -> !response.isAnomaly() && response.getConfidenceScore() == 0.2)
                .verifyComplete();
    }

    @Test
    void testAnalyze_AnomalyDetected() {
        // Return high score for anomaly
        when(ollamaService.predictAnomalyScore(anyString())).thenReturn(Mono.just(0.85));

        AnomalyDetectionRequest request = new AnomalyDetectionRequest();
        request.setHistory(
                List.of(
                        LogEvent.builder()
                                .statusCode(500)
                                .timestamp(1000L)
                                .path("/admin")
                                .clientIp("1.1.1.1")
                                .build(),
                        LogEvent.builder()
                                .statusCode(403)
                                .timestamp(2000L)
                                .path("/admin/config")
                                .clientIp("2.2.2.2")
                                .build()));

        StepVerifier.create(anomalyDetectionService.analyze(request))
                .expectNextMatches(
                        response ->
                                response.isAnomaly()
                                        && response.getConfidenceScore() == 0.85
                                        && "AI_BEHAVIORAL_ANOMALY"
                                                .equals(response.getPatternDetected())
                                        && response.getSuggestedBlockMinutes() == 60)
                .verifyComplete();
    }

    @Test
    void testAnalyze_HighSensitivityRoutes() {
        when(ollamaService.predictAnomalyScore(anyString()))
                .thenAnswer(
                        invocation -> {
                            String prompt = invocation.getArgument(0);
                            if (prompt.contains("routeSensitivity=HIGH")) {
                                return Mono.just(0.9);
                            }
                            return Mono.just(0.1);
                        });

        AnomalyDetectionRequest request = new AnomalyDetectionRequest();
        request.setHistory(List.of(LogEvent.builder().path("/admin/users").build()));

        StepVerifier.create(anomalyDetectionService.analyze(request))
                .expectNextMatches(
                        response -> response.isAnomaly() && response.getConfidenceScore() == 0.9)
                .verifyComplete();
    }

    @Test
    void testAnalyze_MediumSensitivityRoutes() {
        when(ollamaService.predictAnomalyScore(anyString()))
                .thenAnswer(
                        invocation -> {
                            String prompt = invocation.getArgument(0);
                            if (prompt.contains("routeSensitivity=MEDIUM")) {
                                return Mono.just(0.5);
                            }
                            return Mono.just(0.1);
                        });

        AnomalyDetectionRequest request = new AnomalyDetectionRequest();
        request.setHistory(List.of(LogEvent.builder().path("/auth/login").build()));

        StepVerifier.create(anomalyDetectionService.analyze(request))
                .expectNextMatches(
                        response -> !response.isAnomaly() && response.getConfidenceScore() == 0.5)
                .verifyComplete();
    }

    @Test
    void testAnalyze_LowSensitivityRoutes() {
        when(ollamaService.predictAnomalyScore(anyString()))
                .thenAnswer(
                        invocation -> {
                            String prompt = invocation.getArgument(0);
                            if (prompt.contains("routeSensitivity=LOW")) {
                                return Mono.just(0.1);
                            }
                            return Mono.just(0.9);
                        });

        AnomalyDetectionRequest request = new AnomalyDetectionRequest();
        request.setHistory(List.of(LogEvent.builder().path("/public/info").build()));

        StepVerifier.create(anomalyDetectionService.analyze(request))
                .expectNextMatches(
                        response -> !response.isAnomaly() && response.getConfidenceScore() == 0.1)
                .verifyComplete();
    }
}
