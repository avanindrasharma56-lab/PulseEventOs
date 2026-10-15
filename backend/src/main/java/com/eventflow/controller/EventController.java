package com.eventflow.controller;

import com.eventflow.dto.*;
import com.eventflow.security.CurrentUser;
import com.eventflow.security.CustomUserDetails;
import com.eventflow.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EventSummaryResponse>>> searchPublishedEvents(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isOnline,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<EventSummaryResponse> events = eventService.searchPublishedEvents(
                categoryId, search, isOnline, city, startDate, endDate, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<EventDetailResponse>> getPublishedEventBySlug(@PathVariable String slug) {
        EventDetailResponse event = eventService.getPublishedEventBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(event));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ApiResponse<EventDetailResponse>> getEventById(
            @PathVariable Long id,
            @CurrentUser CustomUserDetails currentUser
    ) {
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        EventDetailResponse event = eventService.getEventById(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(event));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<EventDetailResponse>> createDraft(
            @Valid @RequestBody CreateEventRequest request,
            @CurrentUser CustomUserDetails currentUser
    ) {
        EventDetailResponse created = eventService.createDraft(request, currentUser.getId());
        return new ResponseEntity<>(ApiResponse.success("Draft event created successfully", created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<EventDetailResponse>> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request,
            @CurrentUser CustomUserDetails currentUser
    ) {
        EventDetailResponse updated = eventService.updateEvent(id, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Event updated successfully", updated));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<EventDetailResponse>> submitForApproval(
            @PathVariable Long id,
            @CurrentUser CustomUserDetails currentUser
    ) {
        EventDetailResponse submitted = eventService.submitForApproval(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Event submitted for admin approval", submitted));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<EventDetailResponse>> publishEvent(
            @PathVariable Long id,
            @CurrentUser CustomUserDetails currentUser
    ) {
        EventDetailResponse published = eventService.publishEvent(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Event is now live and published", published));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<EventDetailResponse>> cancelEvent(
            @PathVariable Long id,
            @CurrentUser CustomUserDetails currentUser
    ) {
        EventDetailResponse cancelled = eventService.cancelEvent(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Event has been cancelled", cancelled));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @PathVariable Long id,
            @CurrentUser CustomUserDetails currentUser
    ) {
        eventService.deleteEvent(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Event deleted permanently", null));
    }

    @GetMapping("/my-events")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<EventSummaryResponse>>> getMyEvents(
            @CurrentUser CustomUserDetails currentUser
    ) {
        List<EventSummaryResponse> events = eventService.getOrganizerEvents(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(events));
    }
}
