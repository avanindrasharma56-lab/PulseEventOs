package com.eventflow.dto;

import java.time.LocalDateTime;

public class LeadRetrievalDto {
    private Long id;
    private Long sponsorId;
    private String sponsorName;
    private Long attendeeId;
    private String ticketCode;
    private String attendeeName;
    private String attendeeEmail;
    private String notes;
    private Integer rating;
    private LocalDateTime scannedAt;

    public LeadRetrievalDto() {}

    public LeadRetrievalDto(Long id, Long sponsorId, String sponsorName, Long attendeeId, String ticketCode, String attendeeName, String attendeeEmail, String notes, Integer rating, LocalDateTime scannedAt) {
        this.id = id;
        this.sponsorId = sponsorId;
        this.sponsorName = sponsorName;
        this.attendeeId = attendeeId;
        this.ticketCode = ticketCode;
        this.attendeeName = attendeeName;
        this.attendeeEmail = attendeeEmail;
        this.notes = notes;
        this.rating = rating;
        this.scannedAt = scannedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSponsorId() { return sponsorId; }
    public void setSponsorId(Long sponsorId) { this.sponsorId = sponsorId; }

    public String getSponsorName() { return sponsorName; }
    public void setSponsorName(String sponsorName) { this.sponsorName = sponsorName; }

    public Long getAttendeeId() { return attendeeId; }
    public void setAttendeeId(Long attendeeId) { this.attendeeId = attendeeId; }

    public String getTicketCode() { return ticketCode; }
    public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }

    public String getAttendeeName() { return attendeeName; }
    public void setAttendeeName(String attendeeName) { this.attendeeName = attendeeName; }

    public String getAttendeeEmail() { return attendeeEmail; }
    public void setAttendeeEmail(String attendeeEmail) { this.attendeeEmail = attendeeEmail; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public LocalDateTime getScannedAt() { return scannedAt; }
    public void setScannedAt(LocalDateTime scannedAt) { this.scannedAt = scannedAt; }
}
