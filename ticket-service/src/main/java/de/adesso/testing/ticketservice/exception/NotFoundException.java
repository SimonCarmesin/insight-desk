package de.adesso.testing.ticketservice.exception;

public abstract class NotFoundException extends RuntimeException {
    protected NotFoundException(String message) {
        super(message);
    }
}