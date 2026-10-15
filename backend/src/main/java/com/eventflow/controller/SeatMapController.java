package com.eventflow.controller;

import com.eventflow.dto.*;
import com.eventflow.entity.VenueSeatSection;
import com.eventflow.security.CurrentUser;
import com.eventflow.security.CustomUserDetails;
import com.eventflow.service.SeatMapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Venue Seat Maps & Reserved Seating", description = "Visual venue seat maps, 5-minute pessimistic hold locks, and seat reservation engine")
public class SeatMapController {

    private final SeatMapService seatMapService;

    public SeatMapController(SeatMapService seatMapService) {
        this.seatMapService = seatMapService;
    }

    @GetMapping("/events/{eventId}/seat-map")
    @Operation(summary = "Get full visual seat map for an event with live hold & reservation statuses")
    public ResponseEntity<ApiResponse<SeatMapDto>> getSeatMap(
            @PathVariable Long eventId,
            @CurrentUser CustomUserDetails currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : null;
        SeatMapDto map = seatMapService.getEventSeatMap(eventId, userId);
        return ResponseEntity.ok(ApiResponse.success(map));
    }

    @PostMapping("/events/{eventId}/seats/{seatId}/hold")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Place a 5-minute temporary hold lock on an individual seat")
    public ResponseEntity<ApiResponse<HoldSeatResponse>> holdSeat(
            @PathVariable Long eventId,
            @PathVariable Long seatId,
            @CurrentUser CustomUserDetails currentUser) {
        HoldSeatResponse response = seatMapService.holdSeat(eventId, seatId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Seat hold acquired for 5 minutes", response));
    }

    @DeleteMapping("/events/{eventId}/seats/{seatId}/hold")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Release a temporary seat hold")
    public ResponseEntity<ApiResponse<Void>> releaseHold(
            @PathVariable Long eventId,
            @PathVariable Long seatId,
            @CurrentUser CustomUserDetails currentUser) {
        seatMapService.releaseSeatHold(eventId, seatId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Seat hold released", null));
    }

    @PostMapping("/venues/{venueId}/sections")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create custom venue seating section with rows and columns (Organizer/Admin)")
    public ResponseEntity<ApiResponse<VenueSeatSection>> createSection(
            @PathVariable Long venueId,
            @RequestParam String name,
            @RequestParam(required = false) Long tierId,
            @RequestParam(defaultValue = "5") int rowCount,
            @RequestParam(defaultValue = "10") int seatsPerRow) {
        VenueSeatSection section = seatMapService.createSectionWithSeats(venueId, name, tierId, rowCount, seatsPerRow);
        return ResponseEntity.ok(ApiResponse.success("Seat section created successfully", section));
    }
}
