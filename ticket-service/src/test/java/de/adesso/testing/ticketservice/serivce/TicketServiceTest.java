package de.adesso.testing.ticketservice.serivce;

import de.adesso.testing.ticketservice.client.UserDto;
import de.adesso.testing.ticketservice.client.UserServiceClient;
import de.adesso.testing.ticketservice.event.TicketCreatedEvent;
import de.adesso.testing.ticketservice.event.TicketEventProducer;
import de.adesso.testing.ticketservice.exception.InvalidTicketDataException;
import de.adesso.testing.ticketservice.exception.TicketNotFoundException;
import de.adesso.testing.ticketservice.exception.UserNotFoundException;
import de.adesso.testing.ticketservice.model.Priority;
import de.adesso.testing.ticketservice.model.Status;
import de.adesso.testing.ticketservice.model.Ticket;
import de.adesso.testing.ticketservice.model.ticketrequests.CreateTicketRequest;
import de.adesso.testing.ticketservice.repository.TicketRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepo ticketRepo;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private TicketEventProducer ticketEventProducer;

    @InjectMocks
    private TicketService ticketService;

    private Ticket openTicket;
    private Ticket closedTicket;

    @BeforeEach
    void setUp() {
        openTicket = new Ticket("Open Ticket", "Desc", Status.OPEN, Priority.LOW, 1L);
        openTicket.setId(1L);

        closedTicket = new Ticket("Closed Ticket", "Desc", Status.CLOSED, Priority.HIGH, 1L);
        closedTicket.setId(2L);
    }

    // --- createTicket ---

    @Test
    void createTicket_withValidData_savesAndReturnsTicket() {
        CreateTicketRequest request = new CreateTicketRequest("Title", "Desc", "OPEN", "LOW", 1L);
        when(userServiceClient.getUserById(1L)).thenReturn(new UserDto(1L, "Alex", "USER"));
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ticket result = ticketService.createTicket(request);

        assertEquals("Title", result.getTitle());
        assertEquals(Status.OPEN, result.getStatus());
        verify(ticketRepo).save(any(Ticket.class));
        verify(ticketEventProducer).publishTicketCreated(any(TicketCreatedEvent.class));
    }

    @Test
    void createTicket_withUnknownUser_throwsException() {
        CreateTicketRequest request = new CreateTicketRequest("Title", "Desc", "OPEN", "LOW", 99L);
        when(userServiceClient.getUserById(99L)).thenThrow(new UserNotFoundException(99L));

        assertThrows(UserNotFoundException.class, () -> ticketService.createTicket(request));
        verify(ticketRepo, never()).save(any());
    }

    // --- getAllTickets ---

    @Test
    void getAllTickets_returnsAllTicketsFromRepo() {
        when(ticketRepo.findAll()).thenReturn(List.of(openTicket, closedTicket));

        List<Ticket> result = ticketService.getAllTickets();

        assertEquals(2, result.size());
        assertTrue(result.contains(openTicket));
    }

    @Test
    void getAllTickets_whenEmpty_returnsEmptyList() {
        when(ticketRepo.findAll()).thenReturn(List.of());

        List<Ticket> result = ticketService.getAllTickets();

        assertTrue(result.isEmpty());
    }

    // --- getTicketById ---

    @Test
    void getTicketById_existingId_returnsTicket() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(openTicket));

        Ticket result = ticketService.getTicketById(1L);

        assertEquals(openTicket, result);
    }

    @Test
    void getTicketById_unknownId_throwsException() {
        when(ticketRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> ticketService.getTicketById(99L));
    }

    // --- updateTicketStatus ---

    @Test
    void updateTicketStatus_fromClosedToOpen_throwsException() {
        when(ticketRepo.findById(2L)).thenReturn(Optional.of(closedTicket));

        assertThrows(InvalidTicketDataException.class,
                () -> ticketService.updateTicketStatus(2L, "OPEN"));
    }

    @Test
    void updateTicketStatus_fromOpenToInProgress_updatesSuccessfully() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(openTicket));
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ticket result = ticketService.updateTicketStatus(1L, "IN_PROGRESS");

        assertEquals(Status.IN_PROGRESS, result.getStatus());
    }

    // --- updateTicketPriority ---

    @Test
    void updateTicketPriority_validPriority_updatesSuccessfully() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(openTicket));
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ticket result = ticketService.updateTicketPriority(1L, "HIGH");

        assertEquals(Priority.HIGH, result.getPriority());
    }

    @Test
    void updateTicketPriority_invalidPriority_throwsException() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(openTicket));

        assertThrows(IllegalArgumentException.class,
                () -> ticketService.updateTicketPriority(1L, "URGENT"));
    }

    // --- updateTicketDescription ---

    @Test
    void updateTicketDescription_validDescription_updatesSuccessfully() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(openTicket));
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ticket result = ticketService.updateTicketDescription(1L, "New description");

        assertEquals("New description", result.getDescription());
    }

    @Test
    void updateTicketDescription_blankDescription_throwsException() {
        assertThrows(InvalidTicketDataException.class,
                () -> ticketService.updateTicketDescription(1L, ""));
    }

    // --- deleteTicket ---

    @Test
    void deleteTicket_existingTicket_deletesSuccessfully() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(openTicket));

        ticketService.deleteTicket(1L);

        verify(ticketRepo).delete(openTicket);
    }

    @Test
    void deleteTicket_unknownTicket_throwsException() {
        when(ticketRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> ticketService.deleteTicket(99L));
        verify(ticketRepo, never()).delete(any());
    }
}