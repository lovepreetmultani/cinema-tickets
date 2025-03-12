package uk.gov.dwp.uc.pairtest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidAccountIdException;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.model.Ticket;
import uk.gov.dwp.uc.pairtest.model.TicketType;
import thirdparty.paymentgateway.TicketPaymentService;
import uk.gov.dwp.uc.pairtest.repository.TicketRepository;
import thirdparty.seatbooking.SeatReservationService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TicketServiceImpl implements TicketService {
    private static final Logger logger = LoggerFactory.getLogger(TicketServiceImpl.class);

    @Autowired
    private TicketPaymentService ticketPaymentService;
    @Autowired
    private SeatReservationService seatReservationService;
    @Autowired
    private TicketRepository ticketRepository;

    private static final int MAX_TICKETS = 25;

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        logger.info("Starting ticket purchase for account ID: {}", accountId);
        validatePurchaseRequest(accountId, ticketTypeRequests);

        var totalCostOfBooking = calculateTotalAmount(ticketTypeRequests);
        var totalNoOfSeats = calculateTotalSeats(ticketTypeRequests);
        logger.info("Total cost: {} and total seats: {}", totalCostOfBooking, totalNoOfSeats);

        makePayment(accountId, totalCostOfBooking);
        reserveSeats(accountId, totalNoOfSeats);

        Stream.of(ticketTypeRequests)
                .map(ticketTypeRequest -> createTicket(accountId, ticketTypeRequest))
                .forEach(ticketRepository::save);
    }

    private Ticket createTicket(Long accountId, TicketTypeRequest ticketTypeRequest) {
        var ticket = new Ticket();
        ticket.setBookingId(UUID.randomUUID().toString());
        ticket.setAccountId(accountId);
        ticket.setTicketType(ticketTypeRequest.getTicketType());
        ticket.setNumberOfTickets(ticketTypeRequest.getNoOfTickets());
        var pricePerTicket = TicketType.valueOf(ticketTypeRequest.getTicketType().name()).getPrice();
        var totalPrice = BigDecimal.valueOf(pricePerTicket).multiply(BigDecimal.valueOf(ticketTypeRequest.getNoOfTickets()));
        ticket.setPrice(totalPrice);
        return ticket;
    }

    private void validatePurchaseRequest(Long accountId, TicketTypeRequest[] ticketTypeRequests) {
        if (accountId == null || accountId <= 0) {
            logger.error("Invalid account ID: {}", accountId);
            throw new InvalidAccountIdException("Account ID must be greater than zero");
        }

        var ticketCounts = Arrays.stream(ticketTypeRequests)
                .peek(request -> {
                    if (request.getNoOfTickets() < 0) {
                        logger.error("Negative ticket count for type: {} with count: {}", request.getTicketType(), request.getNoOfTickets());
                        throw new InvalidPurchaseException("The number of tickets cannot be negative for ticket type");
                    }
                    if (request.getNoOfTickets() == 0) {
                        logger.error("Zero ticket count for type: {}", request.getTicketType());
                        throw new InvalidPurchaseException("Ticket quantity must be greater than zero");
                    }
                })
                .collect(Collectors.toMap(
                        TicketTypeRequest::getTicketType,
                        TicketTypeRequest::getNoOfTickets,
                        Integer::sum
                ));

        var totalTickets = ticketCounts.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        if (totalTickets > MAX_TICKETS) {
            logger.error("Total tickets exceed max limit: {} > {}", totalTickets, MAX_TICKETS);
            throw new InvalidPurchaseException("Total tickets exceed max limit " + MAX_TICKETS + ".");
        }

        if (isChildOrInfantRequested(ticketCounts) && !isAdultRequested(ticketCounts)) {
            logger.error("Invalid purchase: at least one adult ticket must be purchased if child or infant tickets are included.");
            throw new InvalidPurchaseException("At least one adult ticket must be purchased if child or infant tickets are included.");
        }
    }

    private boolean isChildOrInfantRequested(Map<TicketTypeRequest.Type, Integer> ticketCounts) {
        return ticketCounts.getOrDefault(TicketTypeRequest.Type.CHILD, 0) > 0 ||
                ticketCounts.getOrDefault(TicketTypeRequest.Type.INFANT, 0) > 0;
    }

    private boolean isAdultRequested(Map<TicketTypeRequest.Type, Integer> ticketCounts) {
        return ticketCounts.getOrDefault(TicketTypeRequest.Type.ADULT, 0) > 0;
    }

    private int calculateTotalAmount(TicketTypeRequest... ticketTypeRequests) {
        return Arrays.stream(ticketTypeRequests)
                .mapToInt(request -> request.getNoOfTickets() * TicketType.valueOf(request.getTicketType().name()).getPrice())
                .sum();
    }

    private int calculateTotalSeats(TicketTypeRequest... ticketTypeRequests) {
        return Arrays.stream(ticketTypeRequests)
                .filter(request -> request.getTicketType() == TicketTypeRequest.Type.ADULT || request.getTicketType() == TicketTypeRequest.Type.CHILD)
                .mapToInt(TicketTypeRequest::getNoOfTickets)
                .sum();
    }

    private void makePayment(Long accountId, int totalCost) {
        ticketPaymentService.makePayment(accountId, totalCost);
    }

    private void reserveSeats(Long accountId, int totalSeats) {
        seatReservationService.reserveSeat(accountId, totalSeats);
    }
}