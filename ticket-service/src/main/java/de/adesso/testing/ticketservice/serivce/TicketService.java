package de.adesso.testing.ticketservice.serivce;

import de.adesso.testing.ticketservice.client.UserServiceClient;
import de.adesso.testing.ticketservice.exception.InvalidTicketDataException;
import de.adesso.testing.ticketservice.exception.TicketNotFoundException;
import de.adesso.testing.ticketservice.model.ticketrequests.CreateTicketRequest;
import de.adesso.testing.ticketservice.model.Priority;
import de.adesso.testing.ticketservice.model.Status;
import de.adesso.testing.ticketservice.model.Ticket;
import de.adesso.testing.ticketservice.repository.TicketRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketService {

    private final TicketRepo ticketRepo;
    private final UserServiceClient userServiceClient;

    public TicketService(TicketRepo ticketRepo, UserServiceClient userServiceClient) {
        this.ticketRepo = ticketRepo;
        this.userServiceClient = userServiceClient;
    }

    @Transactional
    public Ticket createTicket(CreateTicketRequest request) {
        if (isBlank(request.title()) || isBlank(request.description())
                || isBlank(request.status()) || isBlank(request.priority())
                || request.assignedUserId() == null) {
            throw new InvalidTicketDataException("Missing required ticket fields");
        }

        userServiceClient.getUserById(request.assignedUserId());

        Status status = Status.valueOf(request.status());
        Priority priority = Priority.valueOf(request.priority());

        Ticket ticket = new Ticket(request.title(), request.description(), status, priority, request.assignedUserId());
        return ticketRepo.save(ticket);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public List<Ticket> getAllTickets() {
        return ticketRepo.findAll();
    }

    public Ticket getTicketById(Long id) {
        return ticketRepo.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    @Transactional
    public Ticket updateTicketStatus(Long id, String newStatusRaw) {
        Ticket ticket = ticketRepo.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        Status newStatus = Status.valueOf(newStatusRaw);

        if (ticket.getStatus() == Status.CLOSED
                && (newStatus == Status.OPEN || newStatus == Status.IN_PROGRESS)) {
            throw new InvalidTicketDataException("Cannot change status from CLOSED to " + newStatus);
        }

        ticket.setStatus(newStatus);
        return ticketRepo.save(ticket);
    }

    @Transactional
    public Ticket updateTicketPriority (Long id, String newPriority) {
        Ticket ticket = ticketRepo.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        Priority priority = Priority.valueOf(newPriority);
        ticket.setPriority(priority);
        return ticketRepo.save(ticket);
    }

    @Transactional
    public Ticket updateTicketDescription(Long id, String newDescription) {
        if (newDescription.isEmpty()) {
            throw new InvalidTicketDataException("Description must not be empty");
        }

        Ticket ticket = ticketRepo.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        ticket.setDescription(newDescription);
        return ticketRepo.save(ticket);
    }

    @Transactional
    public void deleteTicket(Long id) {
        Ticket ticket = ticketRepo.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        ticketRepo.delete(ticket);
    }

}
