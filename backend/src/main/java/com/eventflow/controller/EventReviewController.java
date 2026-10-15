package com.eventflow.controller;

import com.eventflow.dto.ApiResponse;
import com.eventflow.dto.CreateReviewRequest;
import com.eventflow.dto.EventReviewSummaryResponse;
import com.eventflow.dto.ReviewResponse;
import com.eventflow.security.CurrentUser;
import com.eventflow.security.CustomUserDetails;
import com.eventflow.service.EventReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}/reviews")
@Tag(name = "Event Reviews", description = "Public ratings, verified attendee badges, and review moderation")
public class EventReviewController {

    private final EventReviewService reviewService;

    public EventReviewController(EventReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    @Operation(summary = "Get reviews and average rating for an event")
    public ResponseEntity<ApiResponse<EventReviewSummaryResponse>> getReviews(@PathVariable Long eventId) {
        EventReviewSummaryResponse response = reviewService.getEventReviews(eventId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit a rating and review for an event")
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateReviewRequest request,
            @CurrentUser CustomUserDetails currentUser) {
        ReviewResponse response = reviewService.createReview(eventId, request, currentUser.getId());
        return new ResponseEntity<>(ApiResponse.success("Review submitted successfully", response), HttpStatus.CREATED);
    }
}
