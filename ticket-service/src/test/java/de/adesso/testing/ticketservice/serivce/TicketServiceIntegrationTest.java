package de.adesso.testing.ticketservice.serivce;

import de.adesso.testing.ticketservice.model.tickets.ticketrequests.CreateTicketRequest;
import de.adesso.testing.ticketservice.model.tickets.Priority;
import de.adesso.testing.ticketservice.model.tickets.Status;
import de.adesso.testing.ticketservice.model.tickets.Ticket;
import de.adesso.testing.ticketservice.model.user.User;
import de.adesso.testing.ticketservice.repository.TicketRepo;
import de.adesso.testing.ticketservice.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class TicketServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepo ticketRepo;

    @Autowired
    private UserRepo userRepo;

    private User testUser;

    @BeforeEach
    void setUp() {
        ticketRepo.deleteAll();
        userRepo.deleteAll();
        testUser = userRepo.save(new User("Alex", "hashedPw"));
    }

    @Test
    void createTicket_persistsToRealDatabaseWithCorrectUser() {
        CreateTicketRequest request = new CreateTicketRequest(
                "Order issue", "Cannot place order", "OPEN", "HIGH", testUser.getId());

        Ticket created = ticketService.createTicket(request);

        Ticket fromDb = ticketRepo.findById(created.getId()).orElseThrow();
        assertEquals("Order issue", fromDb.getTitle());
        assertEquals(testUser.getId(), fromDb.getAssignedUser().getId());
    }

    @Test
    void updateTicketStatus_concurrentUpdate_throwsOptimisticLockException() {
        Ticket ticket = ticketRepo.save(
                new Ticket("Title", "Desc", Status.OPEN, Priority.LOW, testUser));

        // Zwei "Sichten" auf dasselbe Ticket laden, wie zwei parallele Requests
        Ticket firstView = ticketRepo.findById(ticket.getId()).orElseThrow();
        Ticket secondView = ticketRepo.findById(ticket.getId()).orElseThrow();

        // Erste Änderung speichern -> Version wird hochgezählt
        firstView.setStatus(Status.IN_PROGRESS);
        ticketRepo.saveAndFlush(firstView);

        // Zweite Änderung basiert auf veralteter Version -> muss scheitern
        secondView.setStatus(Status.CLOSED);
        assertThrows(org.springframework.orm.ObjectOptimisticLockingFailureException.class,
                () -> ticketRepo.saveAndFlush(secondView));
    }
}