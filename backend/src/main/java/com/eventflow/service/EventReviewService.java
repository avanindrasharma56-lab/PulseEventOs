package com.eventflow.service;

import com.eventflow.dto.CreateReviewRequest;
import com.eventflow.dto.EventReviewSummaryResponse;
import com.eventflow.dto.ReviewResponse;

public interface EventReviewService {
    ReviewResponse createReview(Long eventId, CreateReviewRequest request, Long currentUserId);
    EventReviewSummaryResponse getEventReviews(Long eventId);
    boolean hasUserReviewed(Long eventId, Long currentUserId);
}
