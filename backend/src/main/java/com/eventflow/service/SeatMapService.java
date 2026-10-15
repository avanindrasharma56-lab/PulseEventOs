package com.eventflow.service;

import com.eventflow.dto.*;
import com.eventflow.entity.VenueSeatSection;

public interface SeatMapService {
    SeatMapDto getEventSeatMap(Long eventId, Long currentUserId);
    HoldSeatResponse holdSeat(Long eventId, Long seatId, Long userId);
    void releaseSeatHold(Long eventId, Long seatId, Long userId);
    VenueSeatSection createSectionWithSeats(Long venueId, String sectionName, Long tierId, int rows, int seatsPerRow);
}
