package de.adesso.testing.intakeservice.model;

import java.math.BigDecimal;

/**
 * Vom Intake-Agent strukturierte Auftragsdaten - Felder entsprechen
 * bewusst 1:1 denen von ticket-service's CreateTicketRequest, damit das
 * Frontend das Ergebnis direkt ins Ticket-Formular uebernehmen kann.
 * Nichts hiervon legt selbst ein Ticket an - das bleibt beim Menschen
 * (Formular pruefen, dann "Ticket erstellen" klicken).
 */
public record ExtractedTicket(String title, String description, String clientName, BigDecimal price, String kind) {
}
