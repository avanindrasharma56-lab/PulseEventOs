package com.eventflow.controller;

import com.eventflow.dto.ApiResponse;
import com.eventflow.dto.CreateTicketTierRequest;
import com.eventflow.dto.TicketTierResponse;
import com.eventflow.security.CurrentUser;
import com.eventflow.security.CustomUserDetails;
import com.eventflow.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/tiers")
public class TicketTierController {

    private final TicketService ticketService;

    public TicketTierController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TicketTierResponse>>> getTiersForEvent(@PathVariable Long eventId) {
        List<TicketTierResponse> tiers = ticketService.getTiersForEvent(eventId);
        return ResponseEntity.ok(ApiResponse.success(tiers));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TicketTierResponse>> createTier(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateTicketTierRequest request,
            @CurrentUser CustomUserDetails currentUser
    ) {
        TicketTierResponse created = ticketService.createTier(eventId, request, currentUser.getId());
        return new ResponseEntity<>(ApiResponse.success("Ticket tier created successfully", created), HttpStatus.CREATED);
    }

    @PutMapping("/{tierId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TicketTierResponse>> updateTier(
            @PathVariable Long eventId,
            @PathVariable Long tierId,
            @Valid @RequestBody CreateTicketTierRequest request,
            @CurrentUser CustomUserDetails currentUser
    ) {
        TicketTierResponse updated = ticketService.updateTier(eventId, tierId, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Ticket tier updated successfully", updated));
    }

    @DeleteMapping("/{tierId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTier(
            @PathVariable Long eventId,
            @PathVariable Long tierId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        ticketService.deleteTier(eventId, tierId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Ticket tier deleted successfully", null));
    }
}
