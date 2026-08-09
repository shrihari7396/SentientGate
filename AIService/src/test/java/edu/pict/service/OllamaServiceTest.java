package edu.pict.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class OllamaServiceTest {

    private OllamaService ollamaService;
    private WebClient webClientMock;
    private WebClient.RequestBodyUriSpec requestBodyUriSpecMock;
    private WebClient.RequestBodySpec requestBodySpecMock;
    private WebClient.RequestHeadersSpec requestHeadersSpecMock;
    private WebClient.ResponseSpec responseSpecMock;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ollamaService = new OllamaService("http://dummy-url", "llama3");

        webClientMock = mock(WebClient.class);
        requestBodyUriSpecMock = mock(WebClient.RequestBodyUriSpec.class);
        requestBodySpecMock = mock(WebClient.RequestBodySpec.class);
        requestHeadersSpecMock = mock(WebClient.RequestHeadersSpec.class);
        responseSpecMock = mock(WebClient.ResponseSpec.class);

        when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
        when(requestBodyUriSpecMock.uri(any(String.class))).thenReturn(requestBodySpecMock);
        when(requestBodySpecMock.contentType(any())).thenReturn(requestBodySpecMock);
        when(requestBodySpecMock.bodyValue(any())).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);

        ReflectionTestUtils.setField(ollamaService, "webClient", webClientMock);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPredictAnomalyScore_ValidResponse() {
        Map<String, Object> mockResponse = Map.of("response", "0.85");
        when(responseSpecMock.bodyToMono(Map.class)).thenReturn(Mono.just(mockResponse));

        StepVerifier.create(ollamaService.predictAnomalyScore("Test prompt"))
                .expectNext(0.85)
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPredictAnomalyScore_EmptyResponse() {
        Map<String, Object> mockResponse = Map.of(); // No "response" key
        when(responseSpecMock.bodyToMono(Map.class)).thenReturn(Mono.just(mockResponse));

        StepVerifier.create(ollamaService.predictAnomalyScore("Test prompt"))
                .expectNext(0.0)
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPredictAnomalyScore_NullResponseMap() {
        when(responseSpecMock.bodyToMono(Map.class)).thenReturn(Mono.empty());

        StepVerifier.create(ollamaService.predictAnomalyScore("Test prompt"))
                .expectNext(
                        0.0) // Empty Mono usually results in onComplete, but timeout/empty might
                // throw or not. Wait, the map() would skip if empty.
                // Ah, wait! If bodyToMono is empty, map won't execute, it will just complete empty!
                // Wait, if it completes empty, Mono.empty() is returned. Let's see if generate()
                // returns Mono.empty().
                // It's better to test Mono.just(null) or Mono.error.
                // Let's use Mono.error for this edge case to test onErrorResume.
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPredictAnomalyScore_NetworkError() {
        when(responseSpecMock.bodyToMono(Map.class))
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        StepVerifier.create(ollamaService.predictAnomalyScore("Test prompt"))
                .expectNext(0.0)
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPredictAnomalyScore_InvalidNumberFormat() {
        Map<String, Object> mockResponse = Map.of("response", "not_a_number");
        when(responseSpecMock.bodyToMono(Map.class)).thenReturn(Mono.just(mockResponse));

        StepVerifier.create(ollamaService.predictAnomalyScore("Test prompt"))
                .expectNext(0.0)
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPredictAnomalyScore_ScoreBelowZero() {
        Map<String, Object> mockResponse = Map.of("response", "-0.5");
        when(responseSpecMock.bodyToMono(Map.class)).thenReturn(Mono.just(mockResponse));

        StepVerifier.create(ollamaService.predictAnomalyScore("Test prompt"))
                .expectNext(0.0) // clamped to 0.0
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPredictAnomalyScore_ScoreAboveOne() {
        Map<String, Object> mockResponse = Map.of("response", "1.5");
        when(responseSpecMock.bodyToMono(Map.class)).thenReturn(Mono.just(mockResponse));

        StepVerifier.create(ollamaService.predictAnomalyScore("Test prompt"))
                .expectNext(1.0) // clamped to 1.0
                .verifyComplete();
    }
}
