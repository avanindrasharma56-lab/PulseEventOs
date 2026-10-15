package com.eventflow.controller;

import com.eventflow.dto.ApiResponse;
import com.eventflow.dto.RoomHeatmapDto;
import com.eventflow.dto.SessionCapacityDto;
import com.eventflow.dto.SessionRsvpDto;
import com.eventflow.security.CurrentUser;
import com.eventflow.security.CustomUserDetails;
import com.eventflow.service.BreakoutSessionBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Breakout Sessions & Room Booking", description = "Multi-track session reservations, time conflict resolver, and room heatmaps")
public class BreakoutSessionBookingController {

    private final BreakoutSessionBookingService bookingService;

    public BreakoutSessionBookingController(BreakoutSessionBookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/sessions/{sessionId}/rsvp")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Reserve a seat or join waitlist for a breakout session")
    public ResponseEntity<ApiResponse<SessionRsvpDto>> reserveSeat(
            @PathVariable Long sessionId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        SessionRsvpDto rsvp = bookingService.reserveSeat(sessionId, currentUser.getId());
        String msg = rsvp.getStatus().name().equals("CONFIRMED")
                ? "Seat successfully reserved"
                : "Session is at full capacity; you have been placed on the priority waitlist";
        return ResponseEntity.ok(ApiResponse.success(msg, rsvp));
    }

    @DeleteMapping("/sessions/{sessionId}/rsvp")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Cancel a breakout session reservation and trigger auto-promotion")
    public ResponseEntity<ApiResponse<SessionRsvpDto>> cancelRsvp(
            @PathVariable Long sessionId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        SessionRsvpDto rsvp = bookingService.cancelRsvp(sessionId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Reservation successfully cancelled", rsvp));
    }

    @GetMapping("/events/{eventId}/my-rsvps")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get attendee's personal confirmed and waitlisted breakout reservations")
    public ResponseEntity<ApiResponse<List<SessionRsvpDto>>> getMySchedule(
            @PathVariable Long eventId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        List<SessionRsvpDto> schedule = bookingService.getMySchedule(eventId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(schedule));
    }

    @GetMapping("/sessions/{sessionId}/capacity")
    @Operation(summary = "Get live room seating occupancy and waitlist count for a session")
    public ResponseEntity<ApiResponse<SessionCapacityDto>> getSessionCapacity(@PathVariable Long sessionId) {
        SessionCapacityDto capacity = bookingService.getSessionCapacity(sessionId);
        return ResponseEntity.ok(ApiResponse.success(capacity));
    }

    @GetMapping("/events/{eventId}/sessions/heatmap")
    @Operation(summary = "Get multi-track room capacity heatmap and occupancy load")
    public ResponseEntity<ApiResponse<List<RoomHeatmapDto>>> getRoomCapacityHeatmap(@PathVariable Long eventId) {
        List<RoomHeatmapDto> heatmap = bookingService.getRoomCapacityHeatmap(eventId);
        return ResponseEntity.ok(ApiResponse.success(heatmap));
    }
}
