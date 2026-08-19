package de.adesso.testing.ticketservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Version
    private Long version;

    public Ticket(String title, String description, Status status, Priority priority, Long assignedUserId) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.assignedUserId = assignedUserId;
    }
}