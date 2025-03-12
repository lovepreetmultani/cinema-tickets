package uk.gov.dwp.uc.pairtest.dto;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;

import java.util.List;

public class TicketPurchaseRequest {
    private Long accountId;
    private List<TicketTypeRequest> ticketTypeRequests;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public List<TicketTypeRequest> getTicketTypeRequests() {
        return ticketTypeRequests;
    }

    public void setTicketTypeRequests(List<TicketTypeRequest> ticketTypeRequests) {
        this.ticketTypeRequests = ticketTypeRequests;
    }
}