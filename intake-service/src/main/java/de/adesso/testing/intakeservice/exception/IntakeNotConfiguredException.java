package de.adesso.testing.intakeservice.exception;

/** AI_HUB_API_KEY ist nicht gesetzt - der Intake-Agent ist noch nicht einsatzbereit. */
public class IntakeNotConfiguredException extends RuntimeException {
    public IntakeNotConfiguredException(String message) {
        super(message);
    }
}
