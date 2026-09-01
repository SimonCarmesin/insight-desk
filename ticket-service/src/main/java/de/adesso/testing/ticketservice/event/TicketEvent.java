package de.adesso.testing.ticketservice.event;

public sealed interface TicketEvent permits TicketCreatedEvent, TicketStatusChangedEvent {}