package de.adesso.testing.ticketservice.exception;

public class InvalidUserDataException extends InvalidDataException {
    public InvalidUserDataException(String message) {
        super(message);
    }
}