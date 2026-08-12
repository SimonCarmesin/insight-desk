package de.adesso.testing.ticketservice.controller;

import de.adesso.testing.ticketservice.exception.TicketNotFoundException;
import de.adesso.testing.ticketservice.model.tickets.Priority;
import de.adesso.testing.ticketservice.model.tickets.Status;
import de.adesso.testing.ticketservice.model.tickets.Ticket;
import de.adesso.testing.ticketservice.serivce.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @Test
    void getTicketById_existingId_returns200() throws Exception {
        Ticket ticket = new Ticket("Title", "Desc", Status.OPEN, Priority.LOW, null);
        ticket.setId(1L);
        when(ticketService.getTicketById(1L)).thenReturn(ticket);

        mockMvc.perform(get("/tickets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    void getTicketById_unknownId_returns404() throws Exception {
        when(ticketService.getTicketById(99L)).thenThrow(new TicketNotFoundException(99L));

        mockMvc.perform(get("/tickets/99"))
                .andExpect(status().isNotFound());
    }
}