package de.adesso.testing.notificationservice.event;

public record TicketStatusChangedEvent(Long ticketId, String title, String oldStatus, String newStatus, Long assignedUserId) implements TicketEvent {}