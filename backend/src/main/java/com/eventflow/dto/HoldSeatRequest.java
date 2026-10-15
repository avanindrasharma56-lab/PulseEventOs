package com.eventflow.dto;

import jakarta.validation.constraints.NotNull;

public class HoldSeatRequest {

    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotNull(message = "Seat ID is required")
    private Long seatId;

    public HoldSeatRequest() {}

    public HoldSeatRequest(Long eventId, Long seatId) {
        this.eventId = eventId;
        this.seatId = seatId;
    }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Long getSeatId() { return seatId; }
    public void setSeatId(Long seatId) { this.seatId = seatId; }
}
