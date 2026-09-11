package de.adesso.testing.notificationservice.service;

import de.adesso.testing.notificationservice.event.TicketCreatedEvent;
import de.adesso.testing.notificationservice.event.TicketStatusChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Neues Verhalten: nur der Wechsel auf CLOSED löst eine Email aus - das war
 * vorher genau andersrum (siehe git-history), auf ausdrücklichen Wunsch
 * korrigiert.
 */
@ExtendWith(MockitoExtension.class)
class EmailNotificationSenderTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailNotificationSender sender;

    @Test
    void supports_statusChangedToClosed_returnsTrue() {
        sender = new EmailNotificationSender(mailSender, "till@example.com");
        TicketStatusChangedEvent event = new TicketStatusChangedEvent(1L, "Title", "IN_PROGRESS", "CLOSED", 2L, "Kunde GmbH", BigDecimal.valueOf(120));

        assertTrue(sender.supports(event));
    }

    @Test
    void supports_statusChangedToInProgress_returnsFalse() {
        sender = new EmailNotificationSender(mailSender, "till@example.com");
        TicketStatusChangedEvent event = new TicketStatusChangedEvent(1L, "Title", "OPEN", "IN_PROGRESS", 2L, "Kunde GmbH", BigDecimal.valueOf(120));

        assertFalse(sender.supports(event));
    }

    @Test
    void supports_ticketCreatedEvent_returnsFalse() {
        sender = new EmailNotificationSender(mailSender, "till@example.com");
        TicketCreatedEvent event = new TicketCreatedEvent(1L, "Title", 2L, "Kunde GmbH", BigDecimal.valueOf(120));

        assertFalse(sender.supports(event));
    }

    @Test
    void handle_closedEvent_sendsMailToConfiguredRecipientWithTicketData() {
        sender = new EmailNotificationSender(mailSender, "till@example.com");
        TicketStatusChangedEvent event = new TicketStatusChangedEvent(1L, "Roomtour Video", "IN_PROGRESS", "CLOSED", 2L, "Kunde GmbH", BigDecimal.valueOf(180));

        sender.handle(event, "Alex");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertArrayEquals(new String[]{"till@example.com"}, sent.getTo());
        assertTrue(sent.getText().contains("Kunde GmbH"));
        assertTrue(sent.getText().contains("180"));
    }

    @Test
    void handle_noRecipientConfigured_doesNotCallMailSender() {
        sender = new EmailNotificationSender(mailSender, "");
        TicketStatusChangedEvent event = new TicketStatusChangedEvent(1L, "Title", "IN_PROGRESS", "CLOSED", 2L, "Kunde GmbH", BigDecimal.valueOf(120));

        sender.handle(event, "Alex");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
