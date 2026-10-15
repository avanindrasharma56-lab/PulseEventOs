package com.eventflow.controller;

import com.eventflow.dto.ApiResponse;
import com.eventflow.dto.NetworkingMeetingDto;
import com.eventflow.dto.NetworkingProfileDto;
import com.eventflow.dto.RequestMeetingRequest;
import com.eventflow.dto.UpsertNetworkingProfileRequest;
import com.eventflow.security.CurrentUser;
import com.eventflow.security.CustomUserDetails;
import com.eventflow.service.NetworkingMeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "1-on-1 Networking Hub & B2B Meetings", description = "Attendee profiles, speed matchmaking directory, and scheduled 1-on-1 meetings")
public class NetworkingMeetingController {

    private final NetworkingMeetingService meetingService;

    public NetworkingMeetingController(NetworkingMeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @GetMapping("/events/{eventId}/networking/profile")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get user's networking profile for an event")
    public ResponseEntity<ApiResponse<NetworkingProfileDto>> getMyProfile(
            @PathVariable Long eventId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        NetworkingProfileDto profile = meetingService.getProfile(eventId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Networking profile fetched successfully", profile));
    }

    @PostMapping("/events/{eventId}/networking/profile")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create or update user's networking profile")
    public ResponseEntity<ApiResponse<NetworkingProfileDto>> upsertProfile(
            @PathVariable Long eventId,
            @CurrentUser CustomUserDetails currentUser,
            @Valid @RequestBody UpsertNetworkingProfileRequest request
    ) {
        NetworkingProfileDto profile = meetingService.upsertProfile(eventId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Networking profile updated successfully", profile));
    }

    @GetMapping("/events/{eventId}/networking/attendees")
    @Operation(summary = "Search attendee networking profiles for matchmaking")
    public ResponseEntity<ApiResponse<List<NetworkingProfileDto>>> searchProfiles(
            @PathVariable Long eventId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String interest
    ) {
        List<NetworkingProfileDto> profiles = meetingService.searchProfiles(eventId, query, interest);
        return ResponseEntity.ok(ApiResponse.success("Attendee networking profiles fetched", profiles));
    }

    @PostMapping("/events/{eventId}/networking/meetings")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Request a 1-on-1 networking meeting with table allocation and conflict check")
    public ResponseEntity<ApiResponse<NetworkingMeetingDto>> requestMeeting(
            @PathVariable Long eventId,
            @CurrentUser CustomUserDetails currentUser,
            @Valid @RequestBody RequestMeetingRequest request
    ) {
        NetworkingMeetingDto meeting = meetingService.requestMeeting(eventId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Meeting request dispatched successfully", meeting));
    }

    @GetMapping("/events/{eventId}/networking/meetings")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get current user's requested and accepted 1-on-1 meetings for an event")
    public ResponseEntity<ApiResponse<List<NetworkingMeetingDto>>> getMyMeetings(
            @PathVariable Long eventId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        List<NetworkingMeetingDto> meetings = meetingService.getMyMeetings(eventId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("User meetings fetched", meetings));
    }

    @PutMapping("/networking/meetings/{meetingId}/respond")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Accept or decline an incoming 1-on-1 meeting request")
    public ResponseEntity<ApiResponse<NetworkingMeetingDto>> respondMeeting(
            @PathVariable Long meetingId,
            @CurrentUser CustomUserDetails currentUser,
            @RequestParam boolean accept
    ) {
        NetworkingMeetingDto meeting = meetingService.respondMeeting(meetingId, currentUser.getId(), accept);
        String msg = accept ? "Meeting request accepted" : "Meeting request declined";
        return ResponseEntity.ok(ApiResponse.success(msg, meeting));
    }

    @DeleteMapping("/networking/meetings/{meetingId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Cancel a 1-on-1 meeting")
    public ResponseEntity<ApiResponse<NetworkingMeetingDto>> cancelMeeting(
            @PathVariable Long meetingId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        NetworkingMeetingDto meeting = meetingService.cancelMeeting(meetingId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Meeting cancelled successfully", meeting));
    }
}
