package de.adesso.testing.notificationservice.service;

import de.adesso.testing.notificationservice.event.TicketEvent;

/**
 * Strategy pro Kanal (Email, SMS, ...). Jeder Sender entscheidet selbst,
 * für welche Events er zuständig ist (supports), wohin er schickt
 * (resolveRecipient) und was drinsteht (buildMessage) - so kann z.B. die
 * Email eine ausführliche Nachricht mit Kunde/Preis bauen, während ein
 * anderer Kanal knapper bleiben kann.
 */
public abstract class NotificationSender {

    public final void handle(TicketEvent event, String assignedUserName) {
        String recipient = resolveRecipient(event, assignedUserName);
        String message = buildMessage(event, assignedUserName);
        doSend(recipient, formatMessage(message));
    }

    public abstract boolean supports(TicketEvent event);

    protected abstract String resolveRecipient(TicketEvent event, String assignedUserName);

    protected abstract String buildMessage(TicketEvent event, String assignedUserName);

    protected abstract void doSend(String recipient, String formattedMessage);

    protected String formatMessage(String message) {
        return message;
    }
}
