package com.eventflow.service;

import com.eventflow.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {
    EventDetailResponse createDraft(CreateEventRequest request, Long currentUserId);
    EventDetailResponse updateEvent(Long eventId, UpdateEventRequest request, Long currentUserId);
    EventDetailResponse submitForApproval(Long eventId, Long currentUserId);
    EventDetailResponse publishEvent(Long eventId, Long currentUserId);
    EventDetailResponse cancelEvent(Long eventId, Long currentUserId);
    void deleteEvent(Long eventId, Long currentUserId);
    List<EventSummaryResponse> getOrganizerEvents(Long currentUserId);
    Page<EventSummaryResponse> getPendingEvents(Pageable pageable);
    EventDetailResponse approveEvent(Long eventId, Long adminId);
    EventDetailResponse rejectEvent(Long eventId, RejectEventRequest request, Long adminId);
    Page<EventSummaryResponse> searchPublishedEvents(
            Long categoryId,
            String search,
            Boolean isOnline,
            String city,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
    EventDetailResponse getPublishedEventBySlug(String slug);
    EventDetailResponse getEventById(Long eventId, Long currentUserId);
}
