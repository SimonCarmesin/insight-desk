package de.adesso.testing.notificationservice.event;

public sealed interface TicketEvent permits TicketCreatedEvent, TicketStatusChangedEvent {}