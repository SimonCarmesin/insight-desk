package de.adesso.testing.intakeservice.client;

import java.util.List;

/** Request-Body fuer POST {ai-hub.base-url}/chat/completions (OpenAI-kompatibel). */
public record AiHubChatRequest(String model, List<AiHubMessage> messages, Integer max_completion_tokens,
                                Double temperature) {
}
