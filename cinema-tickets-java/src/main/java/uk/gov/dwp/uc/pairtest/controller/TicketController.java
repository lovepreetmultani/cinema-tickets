package uk.gov.dwp.uc.pairtest.controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.dto.TicketPurchaseRequest;
import uk.gov.dwp.uc.pairtest.service.TicketService;
@RestController
@RequestMapping("/v1/tickets")
public class TicketController {
    private static final Logger logger = LoggerFactory.getLogger(TicketController.class);

    @Autowired
    private TicketService ticketService;

    @PostMapping("/purchase")
    public ResponseEntity<String> purchaseTickets(@RequestBody TicketPurchaseRequest purchaseRequest) {
        try {
            var ticketTypeRequests = purchaseRequest.getTicketTypeRequests().toArray(new TicketTypeRequest[0]);
            logger.info("ticketTypeRequests :{}", ticketTypeRequests);
            ticketService.purchaseTickets(purchaseRequest.getAccountId(), ticketTypeRequests);
            return ResponseEntity.ok().build();
        } catch (InvalidPurchaseException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
