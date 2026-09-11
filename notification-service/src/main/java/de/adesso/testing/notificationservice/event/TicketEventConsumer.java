package de.adesso.testing.notificationservice.event;

import de.adesso.testing.notificationservice.client.UserDto;
import de.adesso.testing.notificationservice.client.UserServiceClient;
import de.adesso.testing.notificationservice.service.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TicketEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TicketEventConsumer.class);

    private final List<NotificationSender> senders;
    private final UserServiceClient userServiceClient;

    public TicketEventConsumer(List<NotificationSender> senders, UserServiceClient userServiceClient) {
        this.senders = senders;
        this.userServiceClient = userServiceClient;
    }

    @KafkaListener(topics = "ticket-events", groupId = "notification-service")
    public void consume(TicketEvent event) {
        List<NotificationSender> matchingSenders = senders.stream()
                .filter(sender -> sender.supports(event))
                .toList();

        if (matchingSenders.isEmpty()) {
            return;
        }

        Long assignedUserId = resolveAssignedUserId(event);
        String recipientName = resolveRecipientName(assignedUserId);

        matchingSenders.forEach(sender -> sender.handle(event, recipientName));
    }

    private Long resolveAssignedUserId(TicketEvent event) {
        return switch (event) {
            case TicketCreatedEvent created -> created.assignedUserId();
            case TicketStatusChangedEvent statusChanged -> statusChanged.assignedUserId();
        };
    }

    private String resolveRecipientName(Long userId) {
        try {
            UserDto user = userServiceClient.getUserById(userId);
            return user.name();
        } catch (Exception e) {
            log.warn("Could not resolve user {}, falling back to generic recipient", userId, e);
            return "user-" + userId;
        }
    }
}
