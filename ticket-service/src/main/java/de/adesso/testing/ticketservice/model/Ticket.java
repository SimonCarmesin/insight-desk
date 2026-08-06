package de.adesso.testing.ticketservice.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity(name = "Tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @ManyToOne
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;
}
