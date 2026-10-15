package com.eventflow.controller;

import com.eventflow.dto.ApiResponse;
import com.eventflow.dto.JoinWaitlistRequest;
import com.eventflow.dto.OrderResponse;
import com.eventflow.dto.WaitlistEntryResponse;
import com.eventflow.security.CurrentUser;
import com.eventflow.security.CustomUserDetails;
import com.eventflow.service.WaitlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/waitlist")
@Tag(name = "Waitlist Engine", description = "FIFO queue management and ticket reallocation for sold-out tiers")
@SecurityRequirement(name = "Bearer Authentication")
public class WaitlistController {

    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @PostMapping("/join")
    @Operation(summary = "Join waitlist for a ticket tier")
    public ResponseEntity<ApiResponse<WaitlistEntryResponse>> joinWaitlist(
            @Valid @RequestBody JoinWaitlistRequest request,
            @CurrentUser CustomUserDetails currentUser) {
        WaitlistEntryResponse response = waitlistService.joinWaitlist(currentUser.getId(), request.getTicketTierId());
        return ResponseEntity.ok(ApiResponse.success("Successfully joined waitlist", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Leave a waitlist")
    public ResponseEntity<ApiResponse<Void>> leaveWaitlist(
            @PathVariable Long id,
            @CurrentUser CustomUserDetails currentUser) {
        waitlistService.leaveWaitlist(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Left waitlist successfully", null));
    }

    @GetMapping("/my-entries")
    @Operation(summary = "Get current user's waitlist entries")
    public ResponseEntity<ApiResponse<List<WaitlistEntryResponse>>> getMyWaitlistEntries(
            @CurrentUser CustomUserDetails currentUser) {
        List<WaitlistEntryResponse> entries = waitlistService.getMyWaitlistEntries(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(entries));
    }

    @GetMapping("/tier/{tierId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Get waitlist roster for a ticket tier")
    public ResponseEntity<ApiResponse<List<WaitlistEntryResponse>>> getTierWaitlist(
            @PathVariable Long tierId) {
        List<WaitlistEntryResponse> entries = waitlistService.getTierWaitlist(tierId);
        return ResponseEntity.ok(ApiResponse.success(entries));
    }

    @PostMapping("/{id}/claim")
    @Operation(summary = "Claim an active ticket reservation offer from waitlist")
    public ResponseEntity<ApiResponse<OrderResponse>> claimWaitlistOffer(
            @PathVariable Long id,
            @CurrentUser CustomUserDetails currentUser) {
        OrderResponse order = waitlistService.claimWaitlistOffer(
                currentUser.getId(), id, currentUser.getFullName(), currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Ticket pass claimed from waitlist", order));
    }
}
