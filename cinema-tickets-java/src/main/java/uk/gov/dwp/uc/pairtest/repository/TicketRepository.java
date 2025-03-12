package uk.gov.dwp.uc.pairtest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.dwp.uc.pairtest.model.Ticket;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
}