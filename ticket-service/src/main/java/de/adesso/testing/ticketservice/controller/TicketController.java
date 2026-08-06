package de.adesso.testing.ticketservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TicketController {

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
