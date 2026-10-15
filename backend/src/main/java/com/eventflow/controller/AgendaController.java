package com.eventflow.controller;

import com.eventflow.dto.*;
import com.eventflow.security.CurrentUser;
import com.eventflow.security.CustomUserDetails;
import com.eventflow.service.AgendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Conference Agenda & Speakers", description = "Multi-track sessions, speaker directories, attendee schedule bookmarking, and RFC 5545 iCalendar (.ics) exports")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    // --- SPEAKERS ---

    @GetMapping("/{eventId}/speakers")
    @Operation(summary = "Get all speakers for an event")
    public ResponseEntity<ApiResponse<List<SpeakerDto>>> getSpeakers(@PathVariable Long eventId) {
        List<SpeakerDto> speakers = agendaService.getSpeakersByEvent(eventId);
        return ResponseEntity.ok(ApiResponse.success(speakers));
    }

    @PostMapping("/{eventId}/speakers")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Register a new conference speaker (Organizer or Admin)")
    public ResponseEntity<ApiResponse<SpeakerDto>> createSpeaker(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateSpeakerRequest request,
            @CurrentUser CustomUserDetails currentUser) {
        request.setEventId(eventId);
        SpeakerDto created = agendaService.createSpeaker(request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Speaker registered successfully", created));
    }

    @DeleteMapping("/speakers/{speakerId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Delete speaker (Organizer or Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteSpeaker(
            @PathVariable Long speakerId,
            @CurrentUser CustomUserDetails currentUser) {
        agendaService.deleteSpeaker(speakerId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Speaker deleted successfully", null));
    }

    // --- SESSIONS & AGENDA ---

    @GetMapping("/{eventId}/sessions")
    @Operation(summary = "Get full event agenda / track sessions with bookmark status")
    public ResponseEntity<ApiResponse<List<EventSessionDto>>> getSessions(
            @PathVariable Long eventId,
            @CurrentUser CustomUserDetails currentUser) {
        Long userId = (currentUser != null) ? currentUser.getId() : null;
        List<EventSessionDto> sessions = agendaService.getSessionsByEvent(eventId, userId);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @PostMapping("/{eventId}/sessions")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Add an agenda session / talk (Organizer or Admin)")
    public ResponseEntity<ApiResponse<EventSessionDto>> createSession(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateSessionRequest request,
            @CurrentUser CustomUserDetails currentUser) {
        request.setEventId(eventId);
        EventSessionDto created = agendaService.createSession(request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Session added to agenda", created));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Delete session from agenda (Organizer or Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @PathVariable Long sessionId,
            @CurrentUser CustomUserDetails currentUser) {
        agendaService.deleteSession(sessionId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Session removed from agenda", null));
    }

    // --- ATTENDEE BOOKMARKS ---

    @PostMapping("/sessions/{sessionId}/bookmark")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Toggle session bookmark in attendee's personal schedule")
    public ResponseEntity<ApiResponse<BookmarkToggleResponse>> toggleBookmark(
            @PathVariable Long sessionId,
            @CurrentUser CustomUserDetails currentUser) {
        BookmarkToggleResponse res = agendaService.toggleSessionBookmark(currentUser.getId(), sessionId);
        return ResponseEntity.ok(ApiResponse.success(res.getMessage(), res));
    }

    @GetMapping("/{eventId}/my-schedule")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get attendee's personal bookmarked schedule for an event")
    public ResponseEntity<ApiResponse<List<EventSessionDto>>> getMySchedule(
            @PathVariable Long eventId,
            @CurrentUser CustomUserDetails currentUser) {
        List<EventSessionDto> schedule = agendaService.getMyBookmarkedSessions(currentUser.getId(), eventId);
        return ResponseEntity.ok(ApiResponse.success(schedule));
    }

    // --- iCALENDAR (.ICS) EXPORT ---

    @GetMapping("/{eventId}/export/ical")
    @Operation(summary = "Download RFC 5545 iCalendar (.ics) stream for event agenda or personalized bookmarks")
    public ResponseEntity<byte[]> exportIcal(
            @PathVariable Long eventId,
            @CurrentUser CustomUserDetails currentUser) {
        Long userId = (currentUser != null) ? currentUser.getId() : null;
        String icsContent = agendaService.generateIcsCalendar(eventId, userId);

        byte[] bytes = icsContent.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"event-" + eventId + "-schedule.ics\"")
                .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                .body(bytes);
    }
}
