package de.adesso.testing.notificationservice.service;

import de.adesso.testing.notificationservice.event.TicketEvent;
import de.adesso.testing.notificationservice.event.TicketStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Verschickt genau eine Email, wenn ein Ticket auf CLOSED wechselt - so wie
 * gewünscht ("nur eine Email wenn ein Ticket closed ist"). Empfängeradresse
 * kommt bewusst aus der Umgebungsvariable NOTIFICATION_EMAIL_RECIPIENT
 * (siehe application.properties: notification.email.recipient), nicht aus
 * dem User-Service - der kennt aktuell ohnehin keine Email-Adresse (UserDto
 * hat kein email-Feld) und "assignedUserId" ist hier konzeptionell "wer im
 * Cockpit zuständig ist", nicht "wer benachrichtigt wird".
 */
@Component
public class EmailNotificationSender extends NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    private final JavaMailSender mailSender;
    private final String recipient;

    public EmailNotificationSender(JavaMailSender mailSender,
                                    @Value("${notification.email.recipient:}") String recipient) {
        this.mailSender = mailSender;
        this.recipient = recipient;
    }

    @Override
    public boolean supports(TicketEvent event) {
        return event instanceof TicketStatusChangedEvent statusChanged
                && "CLOSED".equals(statusChanged.newStatus());
    }

    @Override
    protected String resolveRecipient(TicketEvent event, String assignedUserName) {
        return recipient;
    }

    @Override
    protected String buildMessage(TicketEvent event, String assignedUserName) {
        TicketStatusChangedEvent statusChanged = (TicketStatusChangedEvent) event;
        StringBuilder body = new StringBuilder();
        body.append("Ticket '").append(statusChanged.title()).append("' wurde abgeschlossen.\n\n");
        if (statusChanged.clientName() != null && !statusChanged.clientName().isBlank()) {
            body.append("Kunde: ").append(statusChanged.clientName()).append("\n");
        }
        if (statusChanged.price() != null) {
            body.append("Preis: ").append(statusChanged.price()).append(" €\n");
        }
        return body.toString();
    }

    @Override
    protected void doSend(String recipient, String formattedMessage) {
        if (recipient == null || recipient.isBlank()) {
            log.warn("NOTIFICATION_EMAIL_RECIPIENT ist nicht gesetzt - Email wird nicht verschickt. " +
                    "Inhalt wäre gewesen:\n{}", formattedMessage);
            return;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(recipient);
            mail.setSubject("Ticket abgeschlossen");
            mail.setText(formattedMessage);
            mailSender.send(mail);
            log.info("Email zu abgeschlossenem Ticket an {} verschickt.", recipient);
        } catch (MailException e) {
            // Absichtlich nicht die Kafka-Verarbeitung crashen lassen, nur wenn der Mailserver
            // (spring.mail.host/-port/-username/-password) noch nicht konfiguriert ist.
            log.warn("Email-Versand fehlgeschlagen: {}", e.getMessage());
        }
    }
}
