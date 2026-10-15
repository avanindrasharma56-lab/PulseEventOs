package com.eventflow.controller;

import com.eventflow.dto.*;
import com.eventflow.security.CurrentUser;
import com.eventflow.security.CustomUserDetails;
import com.eventflow.service.LiveEngagementService;
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
@Tag(name = "Live Engagement", description = "Live audience interactive Q&A and polling engine")
public class LiveEngagementController {

    private final LiveEngagementService liveEngagementService;

    public LiveEngagementController(LiveEngagementService liveEngagementService) {
        this.liveEngagementService = liveEngagementService;
    }

    // --- Q&A ENDPOINTS ---

    @GetMapping("/events/{eventId}/qa")
    @Operation(summary = "Get all questions for an event session with upvote status")
    public ResponseEntity<ApiResponse<List<QuestionDto>>> getQuestions(
            @PathVariable Long eventId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        String username = currentUser != null ? currentUser.getUsername() : null;
        List<QuestionDto> questions = liveEngagementService.getQuestions(eventId, username);
        return ResponseEntity.ok(ApiResponse.success(questions));
    }

    @PostMapping("/events/{eventId}/qa")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Submit a question to the live session")
    public ResponseEntity<ApiResponse<QuestionDto>> submitQuestion(
            @PathVariable Long eventId,
            @Valid @RequestBody QuestionSubmitRequest request,
            @CurrentUser CustomUserDetails currentUser
    ) {
        QuestionDto question = liveEngagementService.submitQuestion(eventId, currentUser.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Question submitted successfully", question));
    }

    @PostMapping("/qa/{questionId}/upvote")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Toggle upvote on a session question")
    public ResponseEntity<ApiResponse<QuestionDto>> toggleUpvote(
            @PathVariable Long questionId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        QuestionDto question = liveEngagementService.toggleUpvote(questionId, currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Question upvote toggled", question));
    }

    @PutMapping("/qa/{questionId}/status")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Update question status (OPEN, PINNED, ANSWERED, DISMISSED)")
    public ResponseEntity<ApiResponse<QuestionDto>> updateQuestionStatus(
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionStatusUpdateRequest request,
            @CurrentUser CustomUserDetails currentUser
    ) {
        QuestionDto question = liveEngagementService.updateQuestionStatus(questionId, currentUser.getUsername(), request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Question status updated", question));
    }

    // --- POLLS ENDPOINTS ---

    @GetMapping("/events/{eventId}/polls")
    @Operation(summary = "Get all live polls for an event with user vote status and results")
    public ResponseEntity<ApiResponse<List<PollDto>>> getPolls(
            @PathVariable Long eventId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        String username = currentUser != null ? currentUser.getUsername() : null;
        List<PollDto> polls = liveEngagementService.getPolls(eventId, username);
        return ResponseEntity.ok(ApiResponse.success(polls));
    }

    @PostMapping("/events/{eventId}/polls")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create a live audience poll (Organizer or Admin)")
    public ResponseEntity<ApiResponse<PollDto>> createPoll(
            @PathVariable Long eventId,
            @Valid @RequestBody CreatePollRequest request,
            @CurrentUser CustomUserDetails currentUser
    ) {
        PollDto poll = liveEngagementService.createPoll(eventId, currentUser.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Poll created successfully", poll));
    }

    @PostMapping("/polls/{pollId}/vote")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Submit vote in a live poll")
    public ResponseEntity<ApiResponse<PollDto>> voteInPoll(
            @PathVariable Long pollId,
            @Valid @RequestBody PollVoteRequest request,
            @CurrentUser CustomUserDetails currentUser
    ) {
        PollDto poll = liveEngagementService.voteInPoll(pollId, currentUser.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Vote recorded successfully", poll));
    }

    @PutMapping("/polls/{pollId}/status")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Update poll status (DRAFT, ACTIVE, CLOSED)")
    public ResponseEntity<ApiResponse<PollDto>> updatePollStatus(
            @PathVariable Long pollId,
            @Valid @RequestBody PollStatusUpdateRequest request,
            @CurrentUser CustomUserDetails currentUser
    ) {
        PollDto poll = liveEngagementService.updatePollStatus(pollId, currentUser.getUsername(), request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Poll status updated", poll));
    }
}
