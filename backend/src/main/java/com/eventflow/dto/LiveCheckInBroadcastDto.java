package com.eventflow.dto;

import java.time.LocalDateTime;

public class LiveCheckInBroadcastDto {
    private String ticketCode;
    private String attendeeName;
    private String attendeeEmail;
    private Long eventId;
    private String eventTitle;
    private String tierName;
    private LocalDateTime checkInTime;
    private int totalCheckedIn;
    private int totalTickets;
    private double checkInPercentage;
    private int gateVelocity; // check-ins per 15 minutes

    public LiveCheckInBroadcastDto() {}

    public LiveCheckInBroadcastDto(String ticketCode, String attendeeName, String attendeeEmail,
                                   Long eventId, String eventTitle, String tierName,
                                   LocalDateTime checkInTime, int totalCheckedIn, int totalTickets,
                                   double checkInPercentage, int gateVelocity) {
        this.ticketCode = ticketCode;
        this.attendeeName = attendeeName;
        this.attendeeEmail = attendeeEmail;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.tierName = tierName;
        this.checkInTime = checkInTime;
        this.totalCheckedIn = totalCheckedIn;
        this.totalTickets = totalTickets;
        this.checkInPercentage = checkInPercentage;
        this.gateVelocity = gateVelocity;
    }

    public String getTicketCode() { return ticketCode; }
    public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }

    public String getAttendeeName() { return attendeeName; }
    public void setAttendeeName(String attendeeName) { this.attendeeName = attendeeName; }

    public String getAttendeeEmail() { return attendeeEmail; }
    public void setAttendeeEmail(String attendeeEmail) { this.attendeeEmail = attendeeEmail; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }

    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }

    public int getTotalCheckedIn() { return totalCheckedIn; }
    public void setTotalCheckedIn(int totalCheckedIn) { this.totalCheckedIn = totalCheckedIn; }

    public int getTotalTickets() { return totalTickets; }
    public void setTotalTickets(int totalTickets) { this.totalTickets = totalTickets; }

    public double getCheckInPercentage() { return checkInPercentage; }
    public void setCheckInPercentage(double checkInPercentage) { this.checkInPercentage = checkInPercentage; }

    public int getGateVelocity() { return gateVelocity; }
    public void setGateVelocity(int gateVelocity) { this.gateVelocity = gateVelocity; }
}
