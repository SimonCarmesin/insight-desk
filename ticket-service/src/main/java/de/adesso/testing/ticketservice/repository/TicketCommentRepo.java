package de.adesso.testing.ticketservice.repository;

import de.adesso.testing.ticketservice.model.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketCommentRepo extends JpaRepository<TicketComment, Long> {

    List<TicketComment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
