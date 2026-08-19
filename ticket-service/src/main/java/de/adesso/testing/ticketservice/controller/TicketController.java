package de.adesso.testing.ticketservice.controller;

import de.adesso.testing.ticketservice.model.ticketrequests.CreateTicketRequest;
import de.adesso.testing.ticketservice.model.Ticket;
import de.adesso.testing.ticketservice.model.ticketrequests.UpdateStatusRequest;
import de.adesso.testing.ticketservice.model.ticketrequests.UpdateTicketDescriptionRequest;
import de.adesso.testing.ticketservice.serivce.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }


    @PostMapping("/tickets")
    public ResponseEntity<Ticket> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        Ticket createdTicket = ticketService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTicket);
    }

    @GetMapping("/tickets")
    public ResponseEntity<List<Ticket>> getTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @PatchMapping("/tickets/{id}/status")
    public ResponseEntity<Ticket> updateTicketStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(ticketService.updateTicketStatus(id, request.newStatus()));
    }

    @PatchMapping("/tickets/{id}/priority")
    public ResponseEntity<Ticket> updateTicketPriority(@PathVariable Long id, @RequestParam String priority) {
        return ResponseEntity.ok(ticketService.updateTicketPriority(id, priority));
    }

    @PutMapping("/tickets/{id}")
    public ResponseEntity<Ticket> updateTicket(@PathVariable Long id, @RequestBody UpdateTicketDescriptionRequest request) {
        return ResponseEntity.ok(ticketService.updateTicketDescription(id, request.newDescription()));
    }

    @DeleteMapping("/tickets/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }
}
