package edu.pict.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import edu.pict.dtos.AnomalyDetectionRequest;
import edu.pict.dtos.AnomalyDetectionResponse;
import edu.pict.service.AnomalyDetectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(AnomalyController.class)
class AnomalyControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AnomalyDetectionService anomalyService;

    @Test
    void testAnalyze_Success() {
        AnomalyDetectionResponse mockResponse = AnomalyDetectionResponse.builder()
                .anomaly(true)
                .confidence(0.9)
                .modelVersion("v1.0")
                .inferenceTimeMs(150)
                .isAnomaly(true)
                .confidenceScore(0.9)
                .patternDetected("AI_BEHAVIORAL_ANOMALY")
                .suggestedBlockMinutes(60)
                .build();

        when(anomalyService.analyze(any(AnomalyDetectionRequest.class)))
                .thenReturn(Mono.just(mockResponse));

        AnomalyDetectionRequest request = new AnomalyDetectionRequest();

        webTestClient.post()
                .uri("/api/v1/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.anomaly").isEqualTo(true)
                .jsonPath("$.confidence").isEqualTo(0.9)
                .jsonPath("$.modelVersion").isEqualTo("v1.0")
                .jsonPath("$.inferenceTimeMs").isEqualTo(150)
                .jsonPath("$.confidenceScore").isEqualTo(0.9)
                .jsonPath("$.patternDetected").isEqualTo("AI_BEHAVIORAL_ANOMALY")
                .jsonPath("$.suggestedBlockMinutes").isEqualTo(60);
    }
}
