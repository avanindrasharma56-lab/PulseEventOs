package com.eventflow.service.impl;

import com.eventflow.dto.*;
import com.eventflow.entity.Event;
import com.eventflow.entity.EventCategory;
import com.eventflow.entity.User;
import com.eventflow.entity.Venue;
import com.eventflow.entity.enums.EventStatus;
import com.eventflow.entity.enums.NotificationType;
import com.eventflow.entity.enums.RoleName;
import com.eventflow.exception.BadRequestException;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.EventCategoryRepository;
import com.eventflow.repository.EventRepository;
import com.eventflow.repository.UserRepository;
import com.eventflow.repository.VenueRepository;
import com.eventflow.service.EventService;
import com.eventflow.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventCategoryRepository categoryRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final com.eventflow.service.EmailService emailService;

    public EventServiceImpl(
            EventRepository eventRepository,
            EventCategoryRepository categoryRepository,
            VenueRepository venueRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            com.eventflow.service.EmailService emailService) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.venueRepository = venueRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    @Override
    public EventDetailResponse createDraft(CreateEventRequest request, Long currentUserId) {
        validateEventDates(request.getStartTime(), request.getEndTime());

        User organizer = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        EventCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Venue venue = null;
        if (request.getVenueId() != null) {
            venue = venueRepository.findById(request.getVenueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", request.getVenueId()));
        }

        String slug = generateUniqueSlug(request.getTitle());

        Event event = new Event(
                organizer,
                category,
                venue,
                request.getTitle().trim(),
                slug,
                request.getShortSummary(),
                request.getDescription(),
                request.getBannerImageUrl(),
                request.getStartTime(),
                request.getEndTime()
        );
        event.setFeatured(request.isFeatured());
        event.setStatus(EventStatus.DRAFT);

        Event saved = eventRepository.save(event);
        return mapToDetailResponse(saved);
    }

    @Override
    public EventDetailResponse updateEvent(Long eventId, UpdateEventRequest request, Long currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);

        if (!event.getOrganizer().getId().equals(currentUserId) && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to modify this event");
        }

        if (event.getStatus() != EventStatus.DRAFT && event.getStatus() != EventStatus.REJECTED && !isAdmin) {
            throw new BadRequestException("Only events in DRAFT or REJECTED status can be edited. Current status: " + event.getStatus());
        }

        validateEventDates(request.getStartTime(), request.getEndTime());

        EventCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Venue venue = null;
        if (request.getVenueId() != null) {
            venue = venueRepository.findById(request.getVenueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", request.getVenueId()));
        }

        if (!event.getTitle().equalsIgnoreCase(request.getTitle().trim())) {
            event.setTitle(request.getTitle().trim());
            event.setSlug(generateUniqueSlug(request.getTitle().trim()));
        }

        event.setCategory(category);
        event.setVenue(venue);
        event.setShortSummary(request.getShortSummary());
        event.setDescription(request.getDescription());
        event.setBannerImageUrl(request.getBannerImageUrl());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setFeatured(request.isFeatured());

        Event saved = eventRepository.save(event);
        return mapToDetailResponse(saved);
    }

    @Override
    public EventDetailResponse submitForApproval(Long eventId, Long currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (!event.getOrganizer().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Only the event organizer can submit this event for approval");
        }

        if (event.getStatus() == EventStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Event is already pending admin approval");
        }

        if (event.getStatus() == EventStatus.APPROVED || event.getStatus() == EventStatus.PUBLISHED) {
            throw new BadRequestException("Event is already approved or published");
        }

        if (event.getStatus() != EventStatus.DRAFT && event.getStatus() != EventStatus.REJECTED) {
            throw new BadRequestException("Cannot submit event with status: " + event.getStatus());
        }

        event.setStatus(EventStatus.PENDING_APPROVAL);
        event.setRejectionReason(null);

        Event saved = eventRepository.save(event);
        return mapToDetailResponse(saved);
    }

    @Override
    public EventDetailResponse publishEvent(Long eventId, Long currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);

        if (!event.getOrganizer().getId().equals(currentUserId) && !isAdmin) {
            throw new AccessDeniedException("Only the event organizer or an admin can publish this event");
        }

        if (event.getStatus() != EventStatus.APPROVED) {
            throw new BadRequestException("Only approved events can be published. Current status: " + event.getStatus());
        }

        event.setStatus(EventStatus.PUBLISHED);
        Event saved = eventRepository.save(event);
        return mapToDetailResponse(saved);
    }

    @Override
    public EventDetailResponse cancelEvent(Long eventId, Long currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);

        if (!event.getOrganizer().getId().equals(currentUserId) && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to cancel this event");
        }

        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.COMPLETED) {
            throw new BadRequestException("Event cannot be cancelled from status: " + event.getStatus());
        }

        event.setStatus(EventStatus.CANCELLED);
        Event saved = eventRepository.save(event);
        return mapToDetailResponse(saved);
    }

    @Override
    public void deleteEvent(Long eventId, Long currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);

        if (!event.getOrganizer().getId().equals(currentUserId) && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to delete this event");
        }

        eventRepository.delete(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> getOrganizerEvents(Long currentUserId) {
        return eventRepository.findByOrganizerIdOrderByCreatedAtDesc(currentUserId).stream()
                .map(this::mapToSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventSummaryResponse> getPendingEvents(Pageable pageable) {
        return eventRepository.findByStatus(EventStatus.PENDING_APPROVAL, pageable)
                .map(this::mapToSummaryResponse);
    }

    @Override
    public EventDetailResponse approveEvent(Long eventId, Long adminId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (event.getOrganizer().getId().equals(adminId)) {
            throw new BadRequestException("Self-approval prohibited: An organizer cannot approve their own event");
        }

        if (event.getStatus() != EventStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only events with status PENDING_APPROVAL can be approved. Current status: " + event.getStatus());
        }

        event.setStatus(EventStatus.APPROVED);
        event.setRejectionReason(null);

        Event saved = eventRepository.save(event);

        try {
            notificationService.sendNotification(
                    saved.getOrganizer(),
                    "Event Approved: " + saved.getTitle(),
                    "Your event has passed moderation review and is ready to be published to the public catalog.",
                    NotificationType.EVENT_APPROVED,
                    saved.getId().toString()
            );
            emailService.sendEventStatusUpdateEmail(saved, null);
        } catch (Exception ignored) {}

        return mapToDetailResponse(saved);
    }

    @Override
    public EventDetailResponse rejectEvent(Long eventId, RejectEventRequest request, Long adminId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (event.getStatus() != EventStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only events with status PENDING_APPROVAL can be rejected. Current status: " + event.getStatus());
        }

        event.setStatus(EventStatus.REJECTED);
        event.setRejectionReason(request.getReason().trim());

        Event saved = eventRepository.save(event);

        try {
            notificationService.sendNotification(
                    saved.getOrganizer(),
                    "Action Required: " + saved.getTitle(),
                    "Your event submission was rejected during review. Reason: " + request.getReason().trim(),
                    NotificationType.EVENT_REJECTED,
                    saved.getId().toString()
            );
            emailService.sendEventStatusUpdateEmail(saved, request.getReason().trim());
        } catch (Exception ignored) {}

        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventSummaryResponse> searchPublishedEvents(
            Long categoryId,
            String search,
            Boolean isOnline,
            String city,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        String sanitizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String sanitizedCity = (city != null && !city.trim().isEmpty()) ? city.trim() : null;

        return eventRepository.searchPublishedEvents(
                EventStatus.PUBLISHED,
                categoryId,
                sanitizedSearch,
                isOnline,
                sanitizedCity,
                startDate,
                endDate,
                pageable
        ).map(this::mapToSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EventDetailResponse getPublishedEventBySlug(String slug) {
        Event event = eventRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "slug", slug));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Event with slug '" + slug + "' is not published");
        }

        return mapToDetailResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public EventDetailResponse getEventById(Long eventId, Long currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            if (currentUserId == null) {
                throw new ResourceNotFoundException("Event not found or not published");
            }
            User currentUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

            boolean isAdmin = currentUser.getRoles().stream()
                    .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);

            if (!event.getOrganizer().getId().equals(currentUserId) && !isAdmin) {
                throw new AccessDeniedException("You do not have permission to view this unpublished event");
            }
        }

        return mapToDetailResponse(event);
    }

    private void validateEventDates(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new BadRequestException("Start time and end time must be specified");
        }
        if (!start.isBefore(end)) {
            throw new BadRequestException("Event start time must be strictly before end time");
        }
    }

    private String generateUniqueSlug(String title) {
        if (title == null) title = "event";
        String base = title.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isEmpty()) base = "event";

        String slug = base;
        int count = 1;
        while (eventRepository.existsBySlug(slug)) {
            slug = base + "-" + count++;
        }
        return slug;
    }

    private EventSummaryResponse mapToSummaryResponse(Event event) {
        EventSummaryResponse res = new EventSummaryResponse();
        res.setId(event.getId());
        res.setTitle(event.getTitle());
        res.setSlug(event.getSlug());
        res.setShortSummary(event.getShortSummary());
        res.setBannerImageUrl(event.getBannerImageUrl());
        res.setStartTime(event.getStartTime());
        res.setEndTime(event.getEndTime());
        res.setStatus(event.getStatus().name());
        res.setFeatured(event.isFeatured());

        if (event.getCategory() != null) {
            res.setCategory(new CategoryResponse(
                    event.getCategory().getId(),
                    event.getCategory().getName(),
                    event.getCategory().getSlug(),
                    event.getCategory().getDescription(),
                    event.getCategory().getIconName(),
                    event.getCategory().isActive()
            ));
        }

        if (event.getVenue() != null) {
            res.setVenue(new VenueResponse(
                    event.getVenue().getId(),
                    event.getVenue().getName(),
                    event.getVenue().getAddress(),
                    event.getVenue().getCity(),
                    event.getVenue().getState(),
                    event.getVenue().getPostalCode(),
                    event.getVenue().getCountry(),
                    event.getVenue().getCapacity(),
                    event.getVenue().isOnline(),
                    event.getVenue().getStreamUrl()
            ));
        }

        if (event.getOrganizer() != null) {
            res.setOrganizerId(event.getOrganizer().getId());
            res.setOrganizerName(event.getOrganizer().getFullName());
        }

        return res;
    }

    private EventDetailResponse mapToDetailResponse(Event event) {
        EventDetailResponse res = new EventDetailResponse();
        res.setId(event.getId());
        res.setTitle(event.getTitle());
        res.setSlug(event.getSlug());
        res.setShortSummary(event.getShortSummary());
        res.setDescription(event.getDescription());
        res.setBannerImageUrl(event.getBannerImageUrl());
        res.setStartTime(event.getStartTime());
        res.setEndTime(event.getEndTime());
        res.setStatus(event.getStatus().name());
        res.setRejectionReason(event.getRejectionReason());
        res.setFeatured(event.isFeatured());
        res.setVersion(event.getVersion());
        res.setCreatedAt(event.getCreatedAt());
        res.setUpdatedAt(event.getUpdatedAt());

        if (event.getCategory() != null) {
            res.setCategory(new CategoryResponse(
                    event.getCategory().getId(),
                    event.getCategory().getName(),
                    event.getCategory().getSlug(),
                    event.getCategory().getDescription(),
                    event.getCategory().getIconName(),
                    event.getCategory().isActive()
            ));
        }

        if (event.getVenue() != null) {
            res.setVenue(new VenueResponse(
                    event.getVenue().getId(),
                    event.getVenue().getName(),
                    event.getVenue().getAddress(),
                    event.getVenue().getCity(),
                    event.getVenue().getState(),
                    event.getVenue().getPostalCode(),
                    event.getVenue().getCountry(),
                    event.getVenue().getCapacity(),
                    event.getVenue().isOnline(),
                    event.getVenue().getStreamUrl()
            ));
        }

        if (event.getOrganizer() != null) {
            res.setOrganizerId(event.getOrganizer().getId());
            res.setOrganizerName(event.getOrganizer().getFullName());
            res.setOrganizerEmail(event.getOrganizer().getEmail());
        }

        return res;
    }
}
