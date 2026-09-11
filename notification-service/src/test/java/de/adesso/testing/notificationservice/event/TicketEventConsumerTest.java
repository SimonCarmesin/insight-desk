package de.adesso.testing.notificationservice.event;

import de.adesso.testing.notificationservice.client.UserDto;
import de.adesso.testing.notificationservice.client.UserServiceClient;
import de.adesso.testing.notificationservice.service.NotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketEventConsumerTest {

    @Mock
    private NotificationSender emailSender;

    @Mock
    private NotificationSender smsSender;

    @Mock
    private UserServiceClient userServiceClient;

    private TicketEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TicketEventConsumer(List.of(emailSender, smsSender), userServiceClient);
    }

    @Test
    void consume_ticketCreated_onlySupportingSenderIsUsed() {
        TicketCreatedEvent event = new TicketCreatedEvent(1L, "Title", 2L, "Kunde", BigDecimal.TEN);
        when(emailSender.supports(event)).thenReturn(true);
        when(smsSender.supports(event)).thenReturn(false);
        when(userServiceClient.getUserById(2L)).thenReturn(new UserDto(2L, "Alex", "USER"));

        consumer.consume(event);

        verify(emailSender).handle(eq(event), eq("Alex"));
        verify(smsSender, never()).handle(any(), anyString());
    }

    @Test
    void consume_statusChangedToClosed_onlySmsSenderIsUsed() {
        TicketStatusChangedEvent event = new TicketStatusChangedEvent(1L, "Title", "IN_PROGRESS", "CLOSED", 2L, "Kunde", BigDecimal.TEN);
        when(emailSender.supports(event)).thenReturn(false);
        when(smsSender.supports(event)).thenReturn(true);
        when(userServiceClient.getUserById(2L)).thenReturn(new UserDto(2L, "Alex", "USER"));

        consumer.consume(event);

        verify(smsSender).handle(eq(event), eq("Alex"));
        verify(emailSender, never()).handle(any(), anyString());
    }

    @Test
    void consume_userServiceUnavailable_fallsBackToGenericRecipient() {
        TicketCreatedEvent event = new TicketCreatedEvent(1L, "Title", 2L, "Kunde", BigDecimal.TEN);
        when(emailSender.supports(event)).thenReturn(true);
        when(userServiceClient.getUserById(2L)).thenThrow(new RuntimeException("Connection refused"));

        consumer.consume(event);

        verify(emailSender).handle(eq(event), eq("user-2"));
    }

    @Test
    void consume_noSenderSupportsEvent_noSenderIsCalled() {
        TicketCreatedEvent event = new TicketCreatedEvent(1L, "Title", 2L, "Kunde", BigDecimal.TEN);
        when(emailSender.supports(event)).thenReturn(false);
        when(smsSender.supports(event)).thenReturn(false);

        consumer.consume(event);

        verify(emailSender, never()).handle(any(), anyString());
        verify(smsSender, never()).handle(any(), anyString());
        verify(userServiceClient, never()).getUserById(any());
    }
}
