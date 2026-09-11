package de.adesso.testing.notificationservice.service;

import de.adesso.testing.notificationservice.event.TicketEvent;
import de.adesso.testing.notificationservice.event.TicketCreatedEvent;
import de.adesso.testing.notificationservice.event.TicketStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Unverändert in seiner Logik (weiterhin nur ein Log-Stub) - nur an die neue
 * NotificationSender-Signatur angepasst, damit es weiter kompiliert.
 * Achtung: feuert wie bisher ebenfalls bei CLOSED, also parallel zur neuen
 * Email. Sag Bescheid, falls das raus soll oder eigenständig bleiben soll.
 */
@Component
public class SmsNotificationSender extends NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationSender.class);

    @Override
    public boolean supports(TicketEvent event) {
        return event instanceof TicketStatusChangedEvent statusChanged
                && "CLOSED".equals(statusChanged.newStatus());
    }

    @Override
    protected String resolveRecipient(TicketEvent event, String assignedUserName) {
        return assignedUserName;
    }

    @Override
    protected String buildMessage(TicketEvent event, String assignedUserName) {
        return switch (event) {
            case TicketCreatedEvent created -> "Ticket '" + created.title() + "' wurde erstellt.";
            case TicketStatusChangedEvent statusChanged ->
                    "Ticket '" + statusChanged.title() + "': Status geändert von "
                            + statusChanged.oldStatus() + " zu " + statusChanged.newStatus() + ".";
        };
    }

    @Override
    protected void doSend(String recipient, String formattedMessage) {
        log.info("Sending SMS to {}: {}", recipient, formattedMessage);
    }

    @Override
    protected String formatMessage(String message) {
        return "[SMS] " + message;
    }
}
