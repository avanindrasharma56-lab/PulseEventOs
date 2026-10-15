package com.eventflow.dto;

import com.eventflow.entity.enums.SeatReservationStatus;
import java.time.LocalDateTime;

public class HoldSeatResponse {
    private Long reservationId;
    private Long eventId;
    private Long seatId;
    private String seatCode;
    private SeatReservationStatus status;
    private LocalDateTime expiresAt;
    private long remainingSeconds;

    public HoldSeatResponse() {}

    public HoldSeatResponse(Long reservationId, Long eventId, Long seatId, String seatCode, SeatReservationStatus status, LocalDateTime expiresAt, long remainingSeconds) {
        this.reservationId = reservationId;
        this.eventId = eventId;
        this.seatId = seatId;
        this.seatCode = seatCode;
        this.status = status;
        this.expiresAt = expiresAt;
        this.remainingSeconds = remainingSeconds;
    }

    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Long getSeatId() { return seatId; }
    public void setSeatId(Long seatId) { this.seatId = seatId; }

    public String getSeatCode() { return seatCode; }
    public void setSeatCode(String seatCode) { this.seatCode = seatCode; }

    public SeatReservationStatus getStatus() { return status; }
    public void setStatus(SeatReservationStatus status) { this.status = status; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public long getRemainingSeconds() { return remainingSeconds; }
    public void setRemainingSeconds(long remainingSeconds) { this.remainingSeconds = remainingSeconds; }
}
