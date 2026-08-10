package de.adesso.testing.ticketservice.controller;

import de.adesso.testing.ticketservice.exception.InvalidTicketDataException;
import de.adesso.testing.ticketservice.exception.TicketNotFoundException;
import de.adesso.testing.ticketservice.exception.UserNotFoundException;
import de.adesso.testing.ticketservice.model.CreateTicketRequest;
import de.adesso.testing.ticketservice.model.Ticket;
import de.adesso.testing.ticketservice.serivce.TicketService;
import jakarta.annotation.Nullable;
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

    /**
     * POST /tickets – Ticket anlegen
     * GET /tickets – Liste (mit Filtern nach Status/Priority als Query-Params – gute Übung für JPA-Queries jenseits von findAll())
     * GET /tickets/{id} – Einzelnes Ticket
     * PATCH /tickets/{id}/status – Status ändern (eigener Endpoint statt generischem PUT, weil Statusänderung fachlich ein eigener Vorgang ist – z. B. darf man nicht von CLOSED zurück auf OPEN, das ist eine Business-Regel, kein reines Datenupdate)
     * PUT /tickets/{id} – Titel/Beschreibung bearbeiten
     * DELETE /tickets/{id} – optional, oder du machst stattdessen einen Soft-Delete (deletedAt-Feld) – auch das ist eine gute Design-Entscheidung zum Ausprobieren
     **/

    @PostMapping("/tickets")
    public ResponseEntity<?> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        try {
            Ticket createdTicket = ticketService.createTicket(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTicket);
        } catch (InvalidTicketDataException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/tickets")
    public ResponseEntity<List<Ticket>> getTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<?> getTicketById(@PathVariable Long id) {
        try {
            Ticket ticket = ticketService.getTicketById(id);
            return ResponseEntity.ok(ticket);
        } catch (TicketNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/tickets/{id}/status")
    public ResponseEntity<?> updateTicketStatus(@PathVariable Long id, @RequestParam String status
    ) {
        try {
            Ticket updatedTicket = ticketService.updateTicketStatus(id, status);
            return ResponseEntity.ok(updatedTicket);
        } catch (TicketNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (InvalidTicketDataException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
