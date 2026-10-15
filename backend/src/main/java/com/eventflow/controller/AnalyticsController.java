package com.eventflow.controller;

import com.eventflow.dto.AdminAnalyticsResponse;
import com.eventflow.dto.ApiResponse;
import com.eventflow.dto.EventAnalyticsResponse;
import com.eventflow.dto.OrganizerAnalyticsResponse;
import com.eventflow.security.CurrentUser;
import com.eventflow.security.CustomUserDetails;
import com.eventflow.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/organizer")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrganizerAnalyticsResponse>> getOrganizerAnalytics(
            @CurrentUser CustomUserDetails currentUser
    ) {
        OrganizerAnalyticsResponse response = analyticsService.getOrganizerAnalytics(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/events/{eventId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<EventAnalyticsResponse>> getEventAnalytics(
            @PathVariable Long eventId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        EventAnalyticsResponse response = analyticsService.getEventAnalytics(eventId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminAnalyticsResponse>> getAdminAnalytics() {
        AdminAnalyticsResponse response = analyticsService.getAdminAnalytics();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
