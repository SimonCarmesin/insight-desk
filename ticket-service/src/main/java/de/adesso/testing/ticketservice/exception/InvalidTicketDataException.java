package de.adesso.testing.ticketservice.exception;

public class InvalidTicketDataException extends InvalidDataException {
    public InvalidTicketDataException(String message) {
        super(message);
    }
}