package de.adesso.testing.ticketservice.exception;

public class InvalidTicketDataException extends RuntimeException {
    public InvalidTicketDataException(String message) {
        super(message);
    }
}