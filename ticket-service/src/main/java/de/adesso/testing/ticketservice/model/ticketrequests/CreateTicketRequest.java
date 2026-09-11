package de.adesso.testing.ticketservice.model.ticketrequests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTicketRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String status,
        @NotBlank String priority,
        @NotNull Long assignedUserId,
        // --- Neu für den AI-Gig-Anwendungsfall, alle optional ---
        String clientName,
        BigDecimal price,
        String kind
) {
    /** Bestehender Aufrufweg ohne Gig-Felder - für generische Tickets. */
    public CreateTicketRequest(String title, String description, String status, String priority, Long assignedUserId) {
        this(title, description, status, priority, assignedUserId, null, null, null);
    }
}
