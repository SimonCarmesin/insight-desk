package de.adesso.testing.notificationservice.event;

import java.math.BigDecimal;

public record TicketCreatedEvent(
        Long ticketId,
        String title,
        Long assignedUserId,
        String clientName,
        BigDecimal price
) implements TicketEvent {}
