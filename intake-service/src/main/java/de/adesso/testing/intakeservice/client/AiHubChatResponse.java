package de.adesso.testing.intakeservice.client;

import java.util.List;

/**
 * Ausschnitt der OpenAI-kompatiblen Chat-Completions-Response - uns
 * interessiert nur der Antworttext der ersten Choice, den Rest (usage,
 * finish_reason, ...) ignorieren wir bewusst.
 */
public record AiHubChatResponse(List<Choice> choices) {
    public record Choice(AiHubMessage message) {
    }
}
