package de.adesso.testing.intakeservice.model;

import jakarta.validation.constraints.NotBlank;

/** Was der Nutzer einfuegt: der rohe Text einer gefundenen Auftragsanfrage. */
public record ExtractRequest(@NotBlank String rawText) {
}
