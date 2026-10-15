package com.eventflow.controller;

import com.eventflow.dto.ApiResponse;
import com.eventflow.dto.CheckInRequest;
import com.eventflow.dto.CheckInResponse;
import com.eventflow.dto.TicketResponse;
import com.eventflow.security.CurrentUser;
import com.eventflow.security.CustomUserDetails;
import com.eventflow.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/my-tickets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getMyTickets(
            @CurrentUser CustomUserDetails currentUser
    ) {
        List<TicketResponse> tickets = ticketService.getMyTickets(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @GetMapping("/{ticketCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketByCode(
            @PathVariable String ticketCode,
            @CurrentUser CustomUserDetails currentUser
    ) {
        TicketResponse ticket = ticketService.getTicketByCode(ticketCode, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CheckInResponse>> checkInTicket(
            @Valid @RequestBody CheckInRequest request,
            @CurrentUser CustomUserDetails currentUser
    ) {
        CheckInResponse response = ticketService.checkInTicket(request.getTicketCode(), currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @GetMapping("/event/{eventId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getEventAttendeeTickets(
            @PathVariable Long eventId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        List<TicketResponse> tickets = ticketService.getEventAttendeeTickets(eventId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @GetMapping("/event/{eventId}/door-stats")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<com.eventflow.dto.DoorStatsDto>> getDoorStats(
            @PathVariable Long eventId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        com.eventflow.dto.DoorStatsDto stats = ticketService.getDoorStats(eventId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
