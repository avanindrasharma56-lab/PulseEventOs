package com.eventflow.dto;

import java.util.List;

public class DoorStatsDto {
    private Long eventId;
    private String eventTitle;
    private int totalIssuedTickets;
    private int checkedInCount;
    private int pendingCheckInCount;
    private double checkInPercentage;
    private int arrivalsLast15Min;
    private List<TicketResponse> recentCheckIns;

    public DoorStatsDto() {}

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public int getTotalIssuedTickets() { return totalIssuedTickets; }
    public void setTotalIssuedTickets(int totalIssuedTickets) { this.totalIssuedTickets = totalIssuedTickets; }

    public int getCheckedInCount() { return checkedInCount; }
    public void setCheckedInCount(int checkedInCount) { this.checkedInCount = checkedInCount; }

    public int getPendingCheckInCount() { return pendingCheckInCount; }
    public void setPendingCheckInCount(int pendingCheckInCount) { this.pendingCheckInCount = pendingCheckInCount; }

    public double getCheckInPercentage() { return checkInPercentage; }
    public void setCheckInPercentage(double checkInPercentage) { this.checkInPercentage = checkInPercentage; }

    public int getArrivalsLast15Min() { return arrivalsLast15Min; }
    public void setArrivalsLast15Min(int arrivalsLast15Min) { this.arrivalsLast15Min = arrivalsLast15Min; }

    public List<TicketResponse> getRecentCheckIns() { return recentCheckIns; }
    public void setRecentCheckIns(List<TicketResponse> recentCheckIns) { this.recentCheckIns = recentCheckIns; }
}
