package de.adesso.testing.notificationservice.service;

import de.adesso.testing.notificationservice.event.TicketEvent;
import de.adesso.testing.notificationservice.event.TicketStatusChangedEvent;
import org.springframework.stereotype.Component;

import static org.apache.kafka.common.requests.DeleteAclsResponse.log;

@Component
public class SmsNotificationSender extends NotificationSender {

    @Override
    public boolean supports(TicketEvent event) {
        return event instanceof TicketStatusChangedEvent statusChanged
                && "CLOSED".equals(statusChanged.newStatus());
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