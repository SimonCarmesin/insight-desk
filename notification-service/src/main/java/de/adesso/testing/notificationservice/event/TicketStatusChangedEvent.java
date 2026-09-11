package de.adesso.testing.notificationservice.event;

import java.math.BigDecimal;

public record TicketStatusChangedEvent(
        Long ticketId,
        String title,
        String oldStatus,
        String newStatus,
        Long assignedUserId,
        String clientName,
        BigDecimal price
) implements TicketEvent {}
