package com.eventflow.dto;

import jakarta.validation.constraints.NotNull;

public class JoinWaitlistRequest {

    @NotNull(message = "Ticket tier ID is required")
    private Long ticketTierId;

    public JoinWaitlistRequest() {}

    public JoinWaitlistRequest(Long ticketTierId) {
        this.ticketTierId = ticketTierId;
    }

    public Long getTicketTierId() {
        return ticketTierId;
    }

    public void setTicketTierId(Long ticketTierId) {
        this.ticketTierId = ticketTierId;
    }
}
