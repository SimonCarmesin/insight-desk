package de.adesso.testing.ticketservice.exception;

public class TicketNotFoundException extends NotFoundException {
    public TicketNotFoundException(Long id) {
        super("Ticket not found with id: " + id);
    }
}