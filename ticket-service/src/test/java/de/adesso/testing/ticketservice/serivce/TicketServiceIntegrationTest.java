package de.adesso.testing.ticketservice.serivce;

import de.adesso.testing.ticketservice.AbstractIntegrationTest;
import de.adesso.testing.ticketservice.client.UserDto;
import de.adesso.testing.ticketservice.client.UserServiceClient;
import de.adesso.testing.ticketservice.event.TicketCreatedEvent;
import de.adesso.testing.ticketservice.event.TicketEventProducer;
import de.adesso.testing.ticketservice.model.ticketrequests.CreateTicketRequest;
import de.adesso.testing.ticketservice.model.Priority;
import de.adesso.testing.ticketservice.model.Status;
import de.adesso.testing.ticketservice.model.Ticket;
import de.adesso.testing.ticketservice.repository.TicketRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class TicketServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepo ticketRepo;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @MockitoBean
    private TicketEventProducer ticketEventProducer;

    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        ticketRepo.deleteAll();
        when(userServiceClient.getUserById(TEST_USER_ID))
                .thenReturn(new UserDto(TEST_USER_ID, "Alex", "USER"));
    }

    @Test
    void createTicket_persistsToRealDatabaseWithCorrectUser() {
        CreateTicketRequest request = new CreateTicketRequest(
                "Order issue", "Cannot place order", "OPEN", "HIGH", TEST_USER_ID);

        Ticket created = ticketService.createTicket(request);

        Ticket fromDb = ticketRepo.findById(created.getId()).orElseThrow();
        assertEquals("Order issue", fromDb.getTitle());
        assertEquals(TEST_USER_ID, fromDb.getAssignedUserId());
        verify(ticketEventProducer).publishTicketCreated(any(TicketCreatedEvent.class));
    }

    @Test
    void updateTicketStatus_concurrentUpdate_throwsOptimisticLockException() {
        Ticket ticket = ticketRepo.save(
                new Ticket("Title", "Desc", Status.OPEN, Priority.LOW, TEST_USER_ID));

        Ticket firstView = ticketRepo.findById(ticket.getId()).orElseThrow();
        Ticket secondView = ticketRepo.findById(ticket.getId()).orElseThrow();

        firstView.setStatus(Status.IN_PROGRESS);
        ticketRepo.saveAndFlush(firstView);

        secondView.setStatus(Status.CLOSED);
        assertThrows(org.springframework.orm.ObjectOptimisticLockingFailureException.class,
                () -> ticketRepo.saveAndFlush(secondView));
    }
}