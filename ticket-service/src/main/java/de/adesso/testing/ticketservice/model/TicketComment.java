package de.adesso.testing.ticketservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Ein Eintrag in der Aktivitäts-Zeitleiste eines Tickets (Kommentar eines
 * Agenten oder von dir). Eigene Tabelle statt JSON-Spalte, damit sich das
 * sauber über die Repository-Schicht abfragen lässt (siehe TicketCommentRepo).
 */
@Data
@NoArgsConstructor
@Entity(name = "TicketComments")
public class TicketComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    private String author;

    @Column(length = 2000)
    private String text;

    private Instant createdAt;

    public TicketComment(Ticket ticket, String author, String text) {
        this.ticket = ticket;
        this.author = author;
        this.text = text;
        this.createdAt = Instant.now();
    }
}
