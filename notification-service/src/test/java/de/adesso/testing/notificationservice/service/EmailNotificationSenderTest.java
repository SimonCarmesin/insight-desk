package de.adesso.testing.notificationservice.service;

import de.adesso.testing.notificationservice.event.TicketCreatedEvent;
import de.adesso.testing.notificationservice.event.TicketStatusChangedEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailNotificationSenderTest {

    private final EmailNotificationSender sender = new EmailNotificationSender();

    @Test
    void supports_ticketCreatedEvent_returnsTrue() {
        TicketCreatedEvent event = new TicketCreatedEvent(1L, "Title", 2L);

        assertTrue(sender.supports(event));
    }

    @Test
    void supports_statusChangedToClosed_returnsFalse() {
        TicketStatusChangedEvent event = new TicketStatusChangedEvent(1L, "Title", "OPEN", "CLOSED", 2L);

        assertFalse(sender.supports(event));
    }

    @Test
    void supports_statusChangedToInProgress_returnsTrue() {
        TicketStatusChangedEvent event = new TicketStatusChangedEvent(1L, "Title", "OPEN", "IN_PROGRESS", 2L);

        assertTrue(sender.supports(event));
    }
}