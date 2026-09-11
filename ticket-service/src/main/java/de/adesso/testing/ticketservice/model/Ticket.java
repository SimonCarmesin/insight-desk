package de.adesso.testing.ticketservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "Tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    private Long assignedUserId;

    // --- Neu für den AI-Gig-Anwendungsfall (Auftrags-Cockpit) ---
    // Alle drei nullable: bestehende, generische Tickets (ohne Gig-Kontext)
    // bleiben gültig, ohne dass diese Felder gesetzt sein müssen.

    /** Name des Kunden/Auftraggebers (z.B. aus dem Intake-Agent). */
    private String clientName;

    /** Vereinbarter Preis für den Auftrag. */
    private BigDecimal price;

    /** Art des zu liefernden Ergebnisses (Video/Bild/Text). */
    @Enumerated(EnumType.STRING)
    private TicketKind kind;

    @Version
    private Long version;

    /** Bestehender Konstruktor - unverändert, damit alter Aufrufcode/Tests weiter funktionieren. */
    public Ticket(String title, String description, Status status, Priority priority, Long assignedUserId) {
        this(title, description, status, priority, assignedUserId, null, null, null);
    }

    /** Neuer Konstruktor inkl. Gig-spezifischer Felder. */
    public Ticket(String title, String description, Status status, Priority priority, Long assignedUserId,
                  String clientName, BigDecimal price, TicketKind kind) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.assignedUserId = assignedUserId;
        this.clientName = clientName;
        this.price = price;
        this.kind = kind;
    }
}
