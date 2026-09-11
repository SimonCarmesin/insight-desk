package de.adesso.testing.ticketservice.event;

import java.math.BigDecimal;

public record TicketCreatedEvent(
        Long ticketId,
        String title,
        Long assignedUserId,
        String clientName,
        BigDecimal price
) implements TicketEvent {}
