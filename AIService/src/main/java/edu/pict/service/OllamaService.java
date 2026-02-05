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

    /**
     * Generate text/HTML using default model from application.yml
     */
    public String generate(String prompt) {
        return generateWithModel(defaultModel, prompt);
    }

    /**
     * Generate using a specific model
     */
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
                    .block();

            if (response == null || !response.containsKey("response")) {
                log.error("Ollama returned empty response!");
                return "<html><body><h2>Error: Empty response from Ollama</h2></body></html>";
            }

            return response.get("response").toString();

        } catch (Exception e) {
            log.error("Ollama generate() error: {}", e.getMessage());

            return """
                <html>
                <body>
                    <h2 style='color:red;'>Ollama AI Error</h2>
                    <p>%s</p>
                </body>
                </html>
            """.formatted(e.getMessage());
        }
    }

    /**
     * System prompt + user prompt version
     */
    public String generateWithSystemPrompt(String systemPrompt, String userPrompt) {
        String finalPrompt = systemPrompt + "\n\n" + userPrompt;
        return generate(finalPrompt);
    }



    public double predictAnomalyScore(String prompt) {

        String response = generate(prompt);

        try {
            return Double.parseDouble(response.trim());
        } catch (Exception e) {
            log.error("Invalid AI score: {}", response);
            return 0.0; // fail-safe
        }
    }

}
