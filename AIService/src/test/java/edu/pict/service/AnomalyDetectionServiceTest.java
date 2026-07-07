package edu.pict.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import edu.pict.dtos.AnomalyDetectionRequest;
import edu.pict.dtos.AnomalyDetectionResponse;
import edu.pict.dtos.LogEvent;
import java.util.List;
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
        List<LogEvent> history = List.of(
            LogEvent.builder().clientIp("127.0.0.1").statusCode(500).timestamp(System.currentTimeMillis() - 10000).path("/admin").build(),
            LogEvent.builder().clientIp("127.0.0.2").statusCode(403).timestamp(System.currentTimeMillis()).path("/admin").build()
        );

        AnomalyDetectionRequest request =
                AnomalyDetectionRequest.builder()
                        .uuid("u-test-uuid")
                        .history(history)
                        .build();

        when(ollamaService.predictAnomalyScore(anyString())).thenReturn(0.9);

        AnomalyDetectionResponse response = anomalyDetectionService.analyze(request);

        assertTrue(response.isAnomaly());
        assertEquals(0.9, response.getConfidence());
    }

    @Test
    void analyze_ShouldNotReturnAnomaly_WhenScoreLow() {
        List<LogEvent> history = List.of(
            LogEvent.builder().clientIp("127.0.0.1").statusCode(200).timestamp(System.currentTimeMillis() - 10000).path("/home").build(),
            LogEvent.builder().clientIp("127.0.0.1").statusCode(200).timestamp(System.currentTimeMillis()).path("/home").build()
        );

        AnomalyDetectionRequest request =
                AnomalyDetectionRequest.builder()
                        .uuid("u-test-uuid")
                        .history(history)
                        .build();

        when(ollamaService.predictAnomalyScore(anyString())).thenReturn(0.2);

        AnomalyDetectionResponse response = anomalyDetectionService.analyze(request);

        assertFalse(response.isAnomaly());
        assertEquals(0.2, response.getConfidence());
    }
}
