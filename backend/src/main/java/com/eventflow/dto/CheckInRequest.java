package com.eventflow.dto;

import jakarta.validation.constraints.NotBlank;

public class CheckInRequest {

    @NotBlank(message = "Ticket code is required")
    private String ticketCode;

    public CheckInRequest() {}

    public CheckInRequest(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }
}
