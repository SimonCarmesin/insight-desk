package de.adesso.testing.intakeservice.client;

import de.adesso.testing.intakeservice.exception.ExtractionFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Duenner Wrapper um adessos internen AI Hub - eine OpenAI-kompatible
 * Chat-Completions-API (POST {base-url}/chat/completions, Auth per
 * "Authorization: Bearer {key}"). Kennt nur "schick einen Prompt hin, gib
 * den Text zurueck" - Prompt-Bau und Antwort-Parsing passieren in
 * IntakeService.
 *
 * Bewusst per RestClient direkt implementiert statt ueber das offizielle
 * openai-java SDK: der Service braucht nur diesen einen Call, und so
 * entfaellt eine zusaetzliche Abhaengigkeit, deren genaues Verhalten unter
 * Spring Boot 4 / Jackson 3 sich hier nicht kompilieren-testen liess.
 */
@Component
public class AiHubClient {

    private static final Logger log = LoggerFactory.getLogger(AiHubClient.class);

    private final RestClient restClient;
    private final String model;
    private final int maxCompletionTokens;
    private final double temperature;

    public AiHubClient(
            @Value("${ai-hub.base-url:}") String baseUrl,
            @Value("${ai-hub.api-key:}") String apiKey,
            @Value("${ai-hub.model:deepseek-v4-flash-sovereign}") String model,
            @Value("${ai-hub.max-completion-tokens:1024}") int maxCompletionTokens,
            @Value("${ai-hub.temperature:0.2}") double temperature
    ) {
        this.model = model;
        this.maxCompletionTokens = maxCompletionTokens;
        this.temperature = temperature;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public String complete(String prompt) {
        try {
            AiHubChatRequest request = new AiHubChatRequest(
                    model,
                    List.of(new AiHubMessage("user", prompt)),
                    maxCompletionTokens,
                    temperature
            );
            AiHubChatResponse response = restClient.post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(AiHubChatResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()
                    || response.choices().get(0).message() == null) {
                throw new ExtractionFailedException("Leere Antwort vom AI Hub erhalten.");
            }
            return response.choices().get(0).message().content();
        } catch (RestClientException e) {
            log.warn("AI-Hub-Call fehlgeschlagen", e);
            throw new ExtractionFailedException("Anfrage an den AI Hub ist fehlgeschlagen: " + e.getMessage(), e);
        }
    }
}
