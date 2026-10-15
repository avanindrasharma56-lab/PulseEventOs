package com.eventflow.dto;

import java.util.List;

public class SeatMapDto {
    private Long eventId;
    private Long venueId;
    private String venueName;
    private Integer totalSeats;
    private Integer availableSeats;
    private Integer heldSeats;
    private Integer reservedSeats;
    private List<SeatSectionDto> sections;

    public SeatMapDto() {}

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Long getVenueId() { return venueId; }
    public void setVenueId(Long venueId) { this.venueId = venueId; }

    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }

    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }

    public Integer getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(Integer availableSeats) { this.availableSeats = availableSeats; }

    public Integer getHeldSeats() { return heldSeats; }
    public void setHeldSeats(Integer heldSeats) { this.heldSeats = heldSeats; }

    public Integer getReservedSeats() { return reservedSeats; }
    public void setReservedSeats(Integer reservedSeats) { this.reservedSeats = reservedSeats; }

    public List<SeatSectionDto> getSections() { return sections; }
    public void setSections(List<SeatSectionDto> sections) { this.sections = sections; }
}
