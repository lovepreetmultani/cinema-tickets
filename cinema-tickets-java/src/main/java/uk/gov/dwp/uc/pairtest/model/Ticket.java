package uk.gov.dwp.uc.pairtest.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import java.math.BigDecimal;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tickets")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String bookingId;

    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type")
    private TicketTypeRequest.Type ticketType;

    private int numberOfTickets;

    private BigDecimal price;
}