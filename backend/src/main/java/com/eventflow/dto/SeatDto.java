package com.eventflow.dto;

import com.eventflow.entity.enums.SeatReservationStatus;

public class SeatDto {
    private Long id;
    private Long sectionId;
    private String rowLabel;
    private Integer seatNumber;
    private String seatCode;
    private SeatReservationStatus status; // null (AVAILABLE), HOLD, RESERVED
    private boolean isMyHold = false;

    public SeatDto() {}

    public SeatDto(Long id, Long sectionId, String rowLabel, Integer seatNumber, String seatCode, SeatReservationStatus status, boolean isMyHold) {
        this.id = id;
        this.sectionId = sectionId;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.seatCode = seatCode;
        this.status = status;
        this.isMyHold = isMyHold;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSectionId() { return sectionId; }
    public void setSectionId(Long sectionId) { this.sectionId = sectionId; }

    public String getRowLabel() { return rowLabel; }
    public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }

    public Integer getSeatNumber() { return seatNumber; }
    public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }

    public String getSeatCode() { return seatCode; }
    public void setSeatCode(String seatCode) { this.seatCode = seatCode; }

    public SeatReservationStatus getStatus() { return status; }
    public void setStatus(SeatReservationStatus status) { this.status = status; }

    public boolean isMyHold() { return isMyHold; }
    public void setMyHold(boolean myHold) { isMyHold = myHold; }
}
