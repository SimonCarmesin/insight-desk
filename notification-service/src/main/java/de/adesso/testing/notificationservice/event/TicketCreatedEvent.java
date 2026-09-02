package de.adesso.testing.notificationservice.event;

public record TicketCreatedEvent(Long ticketId, String title, Long assignedUserId) implements TicketEvent {}