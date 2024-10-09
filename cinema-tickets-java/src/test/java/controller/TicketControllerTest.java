package controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.dwp.uc.pairtest.controller.TicketController;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.service.TicketService;

@ExtendWith(MockitoExtension.class)
public class TicketControllerTest {

    @InjectMocks
    private TicketController ticketController;

    @Mock
    private TicketService ticketService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(ticketController).build();
    }

    @Test
    public void testPurchaseTickets_Success() throws Exception {
        mockMvc.perform(post("/v1/tickets/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\": \"12345\", \"ticketTypeRequests\": [{\"ticketType\": \"ADULT\", \"noOfTickets\": 2}]}")
                )
                .andExpect(status().isOk());

        verify(ticketService, times(1)).purchaseTickets(anyLong(), any());
    }

    @Test
    public void testPurchaseTickets_InvalidPurchaseException() throws Exception {
        doThrow(new InvalidPurchaseException("Invalid number of tickets")).when(ticketService)
                .purchaseTickets(anyLong(), any());

        mockMvc.perform(post("/v1/tickets/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\": \"12345\", \"ticketTypeRequests\": [{\"ticketType\": \"ADULT\", \"noOfTickets\": -1}]}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid number of tickets"));

        verify(ticketService, times(1)).purchaseTickets(anyLong(), any());
    }
}