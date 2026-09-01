package de.adesso.testing.ticketservice.event;

public record TicketCreatedEvent(Long ticketId, String title, Long assignedUserId) implements TicketEvent {}