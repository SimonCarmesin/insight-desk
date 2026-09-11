package de.adesso.testing.ticketservice.serivce;

import de.adesso.testing.ticketservice.client.UserServiceClient;
import de.adesso.testing.ticketservice.event.TicketStatusChangedEvent;
import de.adesso.testing.ticketservice.event.TicketCreatedEvent;
import de.adesso.testing.ticketservice.event.TicketEventProducer;
import de.adesso.testing.ticketservice.exception.InvalidTicketDataException;
import de.adesso.testing.ticketservice.exception.TicketNotFoundException;
import de.adesso.testing.ticketservice.model.TicketComment;
import de.adesso.testing.ticketservice.model.TicketKind;
import de.adesso.testing.ticketservice.model.ticketrequests.AddCommentRequest;
import de.adesso.testing.ticketservice.model.ticketrequests.CreateTicketRequest;
import de.adesso.testing.ticketservice.model.Priority;
import de.adesso.testing.ticketservice.model.Status;
import de.adesso.testing.ticketservice.model.Ticket;
import de.adesso.testing.ticketservice.repository.TicketCommentRepo;
import de.adesso.testing.ticketservice.repository.TicketRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketService {

    private final TicketRepo ticketRepo;
    private final TicketCommentRepo ticketCommentRepo;
    private final UserServiceClient userServiceClient;
    private final TicketEventProducer ticketEventProducer;

    public TicketService(TicketRepo ticketRepo, TicketCommentRepo ticketCommentRepo,
                          UserServiceClient userServiceClient, TicketEventProducer ticketEventProducer) {
        this.ticketRepo = ticketRepo;
        this.ticketCommentRepo = ticketCommentRepo;
        this.userServiceClient = userServiceClient;
        this.ticketEventProducer = ticketEventProducer;
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
        TicketKind kind = isBlank(request.kind()) ? null : TicketKind.valueOf(request.kind());

        Ticket ticket = new Ticket(request.title(), request.description(), status, priority, request.assignedUserId(),
                request.clientName(), request.price(), kind);
        Ticket savedTicket = ticketRepo.save(ticket);

        ticketEventProducer.publishTicketCreated(
                new TicketCreatedEvent(savedTicket.getId(), savedTicket.getTitle(), savedTicket.getAssignedUserId(),
                        savedTicket.getClientName(), savedTicket.getPrice()));

        return savedTicket;
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
        Status oldStatus = ticket.getStatus();

        // IN_REVIEW mit aufgenommen: aus CLOSED darf man auch nicht mehr "zurück in Bearbeitung/Review".
        if (oldStatus == Status.CLOSED
                && (newStatus == Status.OPEN || newStatus == Status.IN_PROGRESS || newStatus == Status.IN_REVIEW)) {
            throw new InvalidTicketDataException("Cannot change status from CLOSED to " + newStatus);
        }

        ticket.setStatus(newStatus);
        Ticket savedTicket = ticketRepo.save(ticket);

        // Bewusst weiterhin nur bei CLOSED: davon hängt aktuell u.a. der Email-Versand in
        // notification-service ab (siehe EmailNotificationSender dort).
        if (newStatus == Status.CLOSED) {
            ticketEventProducer.publishTicketStatus(
                    new TicketStatusChangedEvent(savedTicket.getId(), savedTicket.getTitle(), oldStatus, savedTicket.getStatus(),
                            savedTicket.getAssignedUserId(), savedTicket.getClientName(), savedTicket.getPrice()));
        }

        return savedTicket;
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

    // --- Neu: Aktivitäts-Kommentare (Zeitleiste im Dashboard) ---

    @Transactional
    public TicketComment addComment(Long ticketId, AddCommentRequest request) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        if (isBlank(request.author()) || isBlank(request.text())) {
            throw new InvalidTicketDataException("Comment requires author and text");
        }

        TicketComment comment = new TicketComment(ticket, request.author(), request.text());
        return ticketCommentRepo.save(comment);
    }

    public List<TicketComment> getComments(Long ticketId) {
        // sorgt zugleich dafür, dass eine 404 kommt statt einer leeren Liste bei unbekannter Ticket-ID
        getTicketById(ticketId);
        return ticketCommentRepo.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }
}
