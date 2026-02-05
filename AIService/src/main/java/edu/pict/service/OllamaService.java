package edu.pict.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class OllamaService {

    private final WebClient webClient;
    private final String defaultModel;

    public OllamaService(
            @Value("${ollama.base-url}") String ollamaBaseUrl,
            @Value("${ollama.model}") String defaultModel
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(ollamaBaseUrl)
                .build();
        this.defaultModel = defaultModel;
    }

    public String generate(String prompt) {
        return generateWithModel(defaultModel, prompt);
    }

    @SuppressWarnings("unchecked")
    public String generateWithModel(String model, String prompt) {

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("prompt", prompt);
        payload.put("stream", false);

        try {
            Map<Object, Object> response = webClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(java.time.Duration.ofSeconds(5))
                    .block();

            if (response == null || !response.containsKey("response")) {
                log.error("Ollama returned empty response");
                return "0.0";
            }

            return response.get("response").toString().trim();

        } catch (Exception e) {
            log.error("Ollama generate error", e);
            return "0.0"; // numeric fail-safe
        }
    }

    /**
     * STRICT numeric-only anomaly score [0.0 – 1.0]
     */
    public double predictAnomalyScore(String prompt) {

        String response = generate(prompt);

        try {
            double score = Double.parseDouble(response);

            // Clamp to safe range
            if (score < 0.0) score = 0.0;
            if (score > 1.0) score = 1.0;

            return score;

        } catch (Exception e) {
            log.error("Invalid AI score from Ollama: '{}'", response);
            return 0.0;
        }
    }
}
