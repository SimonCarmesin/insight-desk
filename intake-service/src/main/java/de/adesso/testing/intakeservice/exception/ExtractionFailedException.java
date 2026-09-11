package de.adesso.testing.intakeservice.exception;

/**
 * Der AI-Hub-Call ist fehlgeschlagen, oder die Antwort liess sich nicht
 * in die erwarteten Ticket-Felder umwandeln. In beiden Faellen soll das
 * Frontend einfach auf die manuelle Eingabe zurueckfallen, statt hart zu
 * scheitern.
 */
public class ExtractionFailedException extends RuntimeException {
    public ExtractionFailedException(String message) {
        super(message);
    }

    public ExtractionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
