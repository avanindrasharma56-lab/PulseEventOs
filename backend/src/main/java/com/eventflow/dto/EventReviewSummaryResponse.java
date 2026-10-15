package com.eventflow.dto;

import java.util.ArrayList;
import java.util.List;

public class EventReviewSummaryResponse {
    private Long eventId;
    private Double averageRating;
    private long totalReviews;
    private List<ReviewResponse> reviews = new ArrayList<>();

    public EventReviewSummaryResponse() {}

    public EventReviewSummaryResponse(Long eventId, Double averageRating, long totalReviews, List<ReviewResponse> reviews) {
        this.eventId = eventId;
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.reviews = reviews;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(long totalReviews) {
        this.totalReviews = totalReviews;
    }

    public List<ReviewResponse> getReviews() {
        return reviews;
    }

    public void setReviews(List<ReviewResponse> reviews) {
        this.reviews = reviews;
    }
}
