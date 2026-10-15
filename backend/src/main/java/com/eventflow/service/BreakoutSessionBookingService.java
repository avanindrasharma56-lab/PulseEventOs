package com.eventflow.service;

import com.eventflow.dto.RoomHeatmapDto;
import com.eventflow.dto.SessionCapacityDto;
import com.eventflow.dto.SessionRsvpDto;

import java.util.List;

public interface BreakoutSessionBookingService {

    SessionRsvpDto reserveSeat(Long sessionId, Long userId);

    SessionRsvpDto cancelRsvp(Long sessionId, Long userId);

    List<SessionRsvpDto> getMySchedule(Long eventId, Long userId);

    SessionCapacityDto getSessionCapacity(Long sessionId);

    List<RoomHeatmapDto> getRoomCapacityHeatmap(Long eventId);
}
