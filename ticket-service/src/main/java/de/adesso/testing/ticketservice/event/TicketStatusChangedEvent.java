package de.adesso.testing.ticketservice.event;

import de.adesso.testing.ticketservice.model.Status;

public record TicketStatusChangedEvent(Long ticketId, String title, Status oldStatus, Status newStatus, Long assignedUserId) implements TicketEvent {}