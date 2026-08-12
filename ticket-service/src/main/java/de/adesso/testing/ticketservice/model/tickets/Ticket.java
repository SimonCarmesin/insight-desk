package de.adesso.testing.ticketservice.model.tickets;

import de.adesso.testing.ticketservice.model.user.User;
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

    @Version
    private Long version;

    public Ticket(String title, String description, Status status, Priority priority, User assignedUser) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.assignedUser = assignedUser;
    }
}
