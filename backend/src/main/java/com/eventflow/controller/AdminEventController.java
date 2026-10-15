package com.eventflow.controller;

import com.eventflow.dto.ApiResponse;
import com.eventflow.dto.EventDetailResponse;
import com.eventflow.dto.EventSummaryResponse;
import com.eventflow.dto.RejectEventRequest;
import com.eventflow.security.CurrentUser;
import com.eventflow.security.CustomUserDetails;
import com.eventflow.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/events")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEventController {

    private final EventService eventService;

    public AdminEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<EventSummaryResponse>>> getPendingEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<EventSummaryResponse> pending = eventService.getPendingEvents(pageable);
        return ResponseEntity.ok(ApiResponse.success(pending));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<EventDetailResponse>> approveEvent(
            @PathVariable Long id,
            @CurrentUser CustomUserDetails currentUser
    ) {
        EventDetailResponse approved = eventService.approveEvent(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Event approved successfully", approved));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<EventDetailResponse>> rejectEvent(
            @PathVariable Long id,
            @Valid @RequestBody RejectEventRequest request,
            @CurrentUser CustomUserDetails currentUser
    ) {
        EventDetailResponse rejected = eventService.rejectEvent(id, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Event has been rejected", rejected));
    }
}
