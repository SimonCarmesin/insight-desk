package de.adesso.testing.userservice.exception;

public abstract class InvalidDataException extends RuntimeException {
    protected InvalidDataException(String message) {
        super(message);
    }
}
