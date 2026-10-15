package com.eventflow.service.impl;

import com.eventflow.dto.CreateReviewRequest;
import com.eventflow.dto.EventReviewSummaryResponse;
import com.eventflow.dto.ReviewResponse;
import com.eventflow.entity.Event;
import com.eventflow.entity.EventReview;
import com.eventflow.entity.User;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.EventRepository;
import com.eventflow.repository.EventReviewRepository;
import com.eventflow.repository.TicketRepository;
import com.eventflow.repository.UserRepository;
import com.eventflow.service.EventReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventReviewServiceImpl implements EventReviewService {

    private final EventReviewRepository reviewRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public EventReviewServiceImpl(
            EventReviewRepository reviewRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            TicketRepository ticketRepository) {
        this.reviewRepository = reviewRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public ReviewResponse createReview(Long eventId, CreateReviewRequest request, Long currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        boolean isVerified = ticketRepository.existsByOrderUserIdAndTicketTierEventId(currentUserId, eventId);

        EventReview review = reviewRepository.findByUserIdAndEventId(currentUserId, eventId)
                .orElse(new EventReview(event, user, request.getRating(), request.getTitle(), request.getComment(), isVerified));

        review.setRating(request.getRating());
        review.setTitle(request.getTitle() != null ? request.getTitle().trim() : null);
        review.setComment(request.getComment() != null ? request.getComment().trim() : null);
        review.setVerifiedAttendee(isVerified);

        EventReview saved = reviewRepository.save(review);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EventReviewSummaryResponse getEventReviews(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        List<EventReview> reviews = reviewRepository.findByEventIdOrderByCreatedAtDesc(eventId);
        Double rawAverage = reviewRepository.getAverageRatingByEventId(eventId);
        Double avgRating = rawAverage != null ? BigDecimal.valueOf(rawAverage).setScale(1, RoundingMode.HALF_UP).doubleValue() : 0.0;
        long count = reviews.size();

        List<ReviewResponse> reviewDtos = reviews.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new EventReviewSummaryResponse(eventId, avgRating, count, reviewDtos);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserReviewed(Long eventId, Long currentUserId) {
        return reviewRepository.existsByUserIdAndEventId(currentUserId, eventId);
    }

    private ReviewResponse mapToResponse(EventReview review) {
        ReviewResponse res = new ReviewResponse();
        res.setId(review.getId());
        res.setEventId(review.getEvent().getId());
        res.setUserId(review.getUser().getId());
        res.setAuthorName(review.getUser().getFullName());
        res.setRating(review.getRating());
        res.setTitle(review.getTitle());
        res.setComment(review.getComment());
        res.setVerifiedAttendee(review.isVerifiedAttendee());
        res.setCreatedAt(review.getCreatedAt());
        return res;
    }
}
