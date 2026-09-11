package de.adesso.testing.ticketservice.event;

import de.adesso.testing.ticketservice.model.Status;

import java.math.BigDecimal;

public record TicketStatusChangedEvent(
        Long ticketId,
        String title,
        Status oldStatus,
        Status newStatus,
        Long assignedUserId,
        String clientName,
        BigDecimal price
) implements TicketEvent {}
