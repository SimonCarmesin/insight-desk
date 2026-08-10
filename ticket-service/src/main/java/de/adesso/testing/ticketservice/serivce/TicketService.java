package de.adesso.testing.ticketservice.serivce;

import de.adesso.testing.ticketservice.exception.InvalidTicketDataException;
import de.adesso.testing.ticketservice.exception.TicketNotFoundException;
import de.adesso.testing.ticketservice.exception.UserNotFoundException;
import de.adesso.testing.ticketservice.model.*;
import de.adesso.testing.ticketservice.repository.TicketRepo;
import de.adesso.testing.ticketservice.repository.UserRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketService {

    private final TicketRepo ticketRepo;
    private final UserRepo userRepo;

    public TicketService(TicketRepo ticketRepo, UserRepo userRepo) {
        this.ticketRepo = ticketRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public Ticket createTicket(CreateTicketRequest request) {
        if (isBlank(request.title()) || isBlank(request.description())
                || isBlank(request.status()) || isBlank(request.priority())
                || request.assignedUserId() == null) {
            throw new InvalidTicketDataException("Missing required ticket fields");
        }

        User assignedUser = userRepo.findById(request.assignedUserId())
                .orElseThrow(() -> new UserNotFoundException(request.assignedUserId()));

        Status status = Status.valueOf(request.status());
        Priority priority = Priority.valueOf(request.priority());

        Ticket ticket = new Ticket(request.title(), request.description(), status, priority, assignedUser);
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

    public Ticket updateTicketStatus (Long id, String newStatus) {
        Ticket ticket = ticketRepo.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        Status status = Status.valueOf(newStatus);
        ticket.setStatus(status);
        return ticketRepo.save(ticket);
    }

}
