package de.adesso.testing.intakeservice.client;

/** Eine einzelne Nachricht im OpenAI-kompatiblen Chat-Completions-Request. */
public record AiHubMessage(String role, String content) {
}
