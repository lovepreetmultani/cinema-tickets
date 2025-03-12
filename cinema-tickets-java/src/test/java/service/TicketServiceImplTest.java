package service;

import lombok.SneakyThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidAccountIdException;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.model.Ticket;
import thirdparty.paymentgateway.TicketPaymentService;
import uk.gov.dwp.uc.pairtest.repository.TicketRepository;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.service.TicketServiceImpl;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

public class TicketServiceImplTest {

    @InjectMocks
    private TicketServiceImpl ticketService;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketPaymentService ticketPaymentService;

    @Mock
    private SeatReservationService seatReservationService;

    @Test
    @SneakyThrows
    void testPurchaseTickets_Success() {
        var accountId = 1L;
        var request1 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        var request2 = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);

        doNothing().when(ticketPaymentService).makePayment(anyLong(), anyInt());
        doNothing().when(seatReservationService).reserveSeat(anyLong(), anyInt());

        ticketService.purchaseTickets(accountId, request1, request2);

        verify(ticketPaymentService).makePayment(accountId, 40);
        verify(seatReservationService).reserveSeat(accountId, 2);

        var ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository, times(2)).save(ticketCaptor.capture());

        var capturedTickets = ticketCaptor.getAllValues();
        assertAll("Validate captured tickets",
                () -> assertEquals(2, capturedTickets.size(), "Expected two tickets to be saved."),
                () -> {
                    Ticket adultTicket = capturedTickets.get(0);
                    assertEquals(TicketTypeRequest.Type.ADULT, adultTicket.getTicketType(), "Ticket type should be ADULT");
                    assertEquals(1, adultTicket.getNumberOfTickets(), "Number of tickets should be 1 for ADULT");
                    assertEquals(new BigDecimal(25), adultTicket.getPrice(), "Price should be 25 for ADULT ticket");
                },
                () -> {
                    Ticket childTicket = capturedTickets.get(1);
                    assertEquals(TicketTypeRequest.Type.CHILD, childTicket.getTicketType(), "Ticket type should be CHILD");
                    assertEquals(1, childTicket.getNumberOfTickets(), "Number of tickets should be 1 for CHILD");
                    assertEquals(new BigDecimal(15), childTicket.getPrice(), "Price should be 15 for CHILD ticket");
                }
        );
    }

    @Test
    void testPurchaseTickets_InvalidAccountId_Null() {
        var request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        var exception = assertThrows(InvalidAccountIdException.class, () -> {
            ticketService.purchaseTickets(null, request);
        });

        assertEquals("Account ID must be greater than zero", exception.getMessage());
    }

    @Test
    void testPurchaseTickets_InvalidAccountId_Negative() {
        var accountId = -1L;
        var request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        var exception = assertThrows(InvalidAccountIdException.class, () -> {
            ticketService.purchaseTickets(accountId, request);
        });

        assertEquals("Account ID must be greater than zero", exception.getMessage());
    }

    @Test
    void testPurchaseTickets_NegativeTicketCount() {
        var accountId = 1L;
        var request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, -1);
        var exception = assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(accountId, request);
        });

        assertEquals("The number of tickets cannot be negative for ticket type", exception.getMessage());
    }

    @Test
    void testPurchaseTickets_TotalTicketsExceedLimit() {
        var accountId = 1L;
        var request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 26);
        var exception = assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(accountId, request);
        });

        assertEquals("Total tickets exceed max limit 25.", exception.getMessage());
    }

    @Test
    void testPurchaseTickets_ChildWithoutAdult() {
        var accountId = 1L;
        var request1 = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        var exception = assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(accountId, request1);
        });

        assertEquals("At least one adult ticket must be purchased if child or infant tickets are included.", exception.getMessage());
    }

    @Test
    void testPurchaseTickets_InfantWithoutAdult() {
        var accountId = 1L;
        var request1 = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
        var exception = assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(accountId, request1);
        });

        assertEquals("At least one adult ticket must be purchased if child or infant tickets are included.", exception.getMessage());
    }

    @Test
    void testPurchaseTickets_ZeroTickets() {
        var accountId = 1L;
        var request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);

        var exception = assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(accountId, request);
        });
        assertEquals("Ticket quantity must be greater than zero", exception.getMessage());
        verify(ticketRepository, times(0)).save(any());
    }
}