package com.eventflow.controller;

import com.eventflow.dto.ApiResponse;
import com.eventflow.dto.AwardXpRequest;
import com.eventflow.dto.GamificationProfileDto;
import com.eventflow.dto.LeaderboardEntryDto;
import com.eventflow.security.CurrentUser;
import com.eventflow.security.CustomUserDetails;
import com.eventflow.service.GamificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Gamification, Digital Scavenger Hunt & Leaderboards", description = "Attendee XP points, achievement badges, digital passport booth scans, and public leaderboards")
public class GamificationController {

    private final GamificationService gamificationService;

    public GamificationController(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    @GetMapping("/events/{eventId}/gamification/profile")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get current attendee's gamification profile, XP, tier, badges and missions")
    public ResponseEntity<ApiResponse<GamificationProfileDto>> getProfile(
            @PathVariable Long eventId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        GamificationProfileDto profile = gamificationService.getProfile(eventId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Gamification profile retrieved successfully", profile));
    }

    @GetMapping("/events/{eventId}/gamification/leaderboard")
    @Operation(summary = "Get live public event leaderboard with ranks, XP, and badge counts")
    public ResponseEntity<ApiResponse<List<LeaderboardEntryDto>>> getLeaderboard(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<LeaderboardEntryDto> leaderboard = gamificationService.getLeaderboard(eventId, limit);
        return ResponseEntity.ok(ApiResponse.success("Event leaderboard retrieved successfully", leaderboard));
    }

    @PostMapping("/events/{eventId}/gamification/scan-booth/{sponsorId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Scan a sponsor expo booth QR code to earn XP and Explorer badge")
    public ResponseEntity<ApiResponse<GamificationProfileDto>> scanBooth(
            @PathVariable Long eventId,
            @PathVariable Long sponsorId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        GamificationProfileDto profile = gamificationService.scanBooth(eventId, currentUser.getId(), sponsorId);
        return ResponseEntity.ok(ApiResponse.success("Sponsor booth scanned successfully", profile));
    }

    @PostMapping("/events/{eventId}/gamification/session-checkin/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Check in to a breakout session to earn XP and Scholar badge")
    public ResponseEntity<ApiResponse<GamificationProfileDto>> checkInSession(
            @PathVariable Long eventId,
            @PathVariable Long sessionId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        GamificationProfileDto profile = gamificationService.checkInSession(eventId, currentUser.getId(), sessionId);
        return ResponseEntity.ok(ApiResponse.success("Breakout session checked in successfully", profile));
    }

    @PostMapping("/events/{eventId}/gamification/award-xp")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Award XP for turnstile check-in, Q&A, poll, or networking activity")
    public ResponseEntity<ApiResponse<GamificationProfileDto>> awardXp(
            @PathVariable Long eventId,
            @RequestBody AwardXpRequest request,
            @CurrentUser CustomUserDetails currentUser
    ) {
        GamificationProfileDto profile = gamificationService.awardXp(
                eventId,
                currentUser.getId(),
                request.getActivityType(),
                request.getReferenceId(),
                request.getDescription(),
                request.getCustomXp()
        );
        return ResponseEntity.ok(ApiResponse.success("XP awarded successfully", profile));
    }

    @PostMapping("/events/{eventId}/gamification/claim-swag")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Claim physical VIP swag pass once Gold or Platinum tier is achieved")
    public ResponseEntity<ApiResponse<GamificationProfileDto>> claimSwag(
            @PathVariable Long eventId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        GamificationProfileDto profile = gamificationService.claimSwag(eventId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Swag pass claimed successfully", profile));
    }
}
