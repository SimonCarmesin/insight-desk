package de.adesso.testing.notificationservice.service;

import de.adesso.testing.notificationservice.event.TicketEvent;

public abstract class NotificationSender {

    public final void send(String recipient, String message) {
        String formatted = formatMessage(message);
        doSend(recipient, formatted);
    }

    public abstract boolean supports(TicketEvent event);

    protected abstract void doSend(String recipient, String formattedMessage);

    protected String formatMessage(String message) {
        return message;
    }
}