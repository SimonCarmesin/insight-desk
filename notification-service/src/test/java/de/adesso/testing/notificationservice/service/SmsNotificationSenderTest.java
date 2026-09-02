package de.adesso.testing.notificationservice.service;

import de.adesso.testing.notificationservice.event.TicketCreatedEvent;
import de.adesso.testing.notificationservice.event.TicketStatusChangedEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SmsNotificationSenderTest {

    private final SmsNotificationSender sender = new SmsNotificationSender();

    @Test
    void supports_statusChangedToClosed_returnsTrue() {
        TicketStatusChangedEvent event = new TicketStatusChangedEvent(1L, "Title", "IN_PROGRESS", "CLOSED", 2L);

        assertTrue(sender.supports(event));
    }

    @Test
    void supports_ticketCreatedEvent_returnsFalse() {
        TicketCreatedEvent event = new TicketCreatedEvent(1L, "Title", 2L);

        assertFalse(sender.supports(event));
    }
}