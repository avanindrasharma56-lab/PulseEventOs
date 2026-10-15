package com.eventflow.dto;

public class CheckInResponse {
    private boolean success;
    private String message;
    private TicketResponse ticket;

    public CheckInResponse() {}

    public CheckInResponse(boolean success, String message, TicketResponse ticket) {
        this.success = success;
        this.message = message;
        this.ticket = ticket;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public TicketResponse getTicket() {
        return ticket;
    }

    public void setTicket(TicketResponse ticket) {
        this.ticket = ticket;
    }
}
