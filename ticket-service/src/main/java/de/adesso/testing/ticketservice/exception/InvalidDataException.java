package de.adesso.testing.ticketservice.exception;

public abstract class InvalidDataException extends RuntimeException {
    protected InvalidDataException(String message) {
        super(message);
    }
}
