package de.adesso.testing.ticketservice.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TicketEventProducer {

    private static final String TOPIC = "ticket-events";

    private final KafkaTemplate<String, TicketEvent> kafkaTemplate;

    public TicketEventProducer(KafkaTemplate<String, TicketEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishTicketCreated(TicketCreatedEvent event) {
        kafkaTemplate.send(TOPIC, event.ticketId().toString(), event);
    }

    public void publishTicketStatus(TicketStatusChangedEvent event) {
        kafkaTemplate.send(TOPIC, event.ticketId().toString(), event);
    }
}