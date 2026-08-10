package de.adesso.testing.ticketservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TicketController {


    /**
     *POST /tickets – Ticket anlegen
     * GET /tickets – Liste (mit Filtern nach Status/Priority als Query-Params – gute Übung für JPA-Queries jenseits von findAll())
     * GET /tickets/{id} – Einzelnes Ticket
     * PATCH /tickets/{id}/status – Status ändern (eigener Endpoint statt generischem PUT, weil Statusänderung fachlich ein eigener Vorgang ist – z. B. darf man nicht von CLOSED zurück auf OPEN, das ist eine Business-Regel, kein reines Datenupdate)
     * PUT /tickets/{id} – Titel/Beschreibung bearbeiten
     * DELETE /tickets/{id} – optional, oder du machst stattdessen einen Soft-Delete (deletedAt-Feld) – auch das ist eine gute Design-Entscheidung zum Ausprobieren
     **/

    @PostMapping("/tickets")
    public String createTicket() {
        return "Ticket created";
    }

    @GetMapping("/tickets")
    public String getTickets() {
        return "List of tickets";
    }

    @GetMapping("/tickets/{id}")
    public String getTicketById(@PathVariable final String id) {
        return "Ticket details";
    }

}
