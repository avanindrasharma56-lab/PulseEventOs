package com.eventflow.dto;

import jakarta.validation.constraints.NotBlank;

public class LeadScanRequest {

    @NotBlank(message = "Ticket code or attendee identifier is required")
    private String ticketCode;

    private String notes;
    private Integer rating = 3; // 1 to 5

    public LeadScanRequest() {}

    public LeadScanRequest(String ticketCode, String notes, Integer rating) {
        this.ticketCode = ticketCode;
        this.notes = notes;
        this.rating = rating != null ? rating : 3;
    }

    public String getTicketCode() { return ticketCode; }
    public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
}
