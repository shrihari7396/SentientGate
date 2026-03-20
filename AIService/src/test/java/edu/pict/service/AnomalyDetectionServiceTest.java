package edu.pict.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import edu.pict.dtos.AnomalyDetectionRequest;
import edu.pict.dtos.AnomalyDetectionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    @Mock private OllamaService ollamaService;

    private AnomalyDetectionService anomalyDetectionService;

    @BeforeEach
    void setUp() {
        anomalyDetectionService = new AnomalyDetectionService(ollamaService);
    }

    @Test
    void analyze_ShouldReturnAnomaly_WhenScoreHigh() {
        AnomalyDetectionRequest request =
                AnomalyDetectionRequest.builder()
                        .failureRate(0.8f)
                        .requestsPerMinute(500)
                        .uniqueRoutesAccessed(50)
                        .jwtReuseCount(10)
                        .ipReputationScore(0.1f)
                        .routeSensitivity("HIGH")
                        .build();

        when(ollamaService.predictAnomalyScore(anyString())).thenReturn(0.9);

        AnomalyDetectionResponse response = anomalyDetectionService.analyze(request);

        assertTrue(response.isAnomaly());
        assertEquals(0.9, response.getConfidence());
    }

    @Test
    void analyze_ShouldNotReturnAnomaly_WhenScoreLow() {
        AnomalyDetectionRequest request =
                AnomalyDetectionRequest.builder()
                        .failureRate(0.01f)
                        .requestsPerMinute(10)
                        .uniqueRoutesAccessed(2)
                        .jwtReuseCount(0)
                        .ipReputationScore(0.9f)
                        .routeSensitivity("LOW")
                        .build();

        when(ollamaService.predictAnomalyScore(anyString())).thenReturn(0.2);

        AnomalyDetectionResponse response = anomalyDetectionService.analyze(request);

        assertFalse(response.isAnomaly());
        assertEquals(0.2, response.getConfidence());
    }
}
