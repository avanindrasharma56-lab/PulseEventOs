package com.eventflow.service.impl;

import com.eventflow.dto.RoomHeatmapDto;
import com.eventflow.dto.SessionCapacityDto;
import com.eventflow.dto.SessionRsvpDto;
import com.eventflow.entity.*;
import com.eventflow.entity.enums.NotificationType;
import com.eventflow.entity.enums.RoleName;
import com.eventflow.entity.enums.SessionRsvpStatus;
import com.eventflow.exception.BadRequestException;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.*;
import com.eventflow.service.BreakoutSessionBookingService;
import com.eventflow.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class BreakoutSessionBookingServiceImpl implements BreakoutSessionBookingService {

    private final EventSessionRepository eventSessionRepository;
    private final SessionRsvpRepository sessionRsvpRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final NotificationService notificationService;

    public BreakoutSessionBookingServiceImpl(
            EventSessionRepository eventSessionRepository,
            SessionRsvpRepository sessionRsvpRepository,
            UserRepository userRepository,
            TicketRepository ticketRepository,
            @Autowired(required = false) NotificationService notificationService) {
        this.eventSessionRepository = eventSessionRepository;
        this.sessionRsvpRepository = sessionRsvpRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.notificationService = notificationService;
    }

    @Override
    public SessionRsvpDto reserveSeat(Long sessionId, Long userId) {
        EventSession session = eventSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("EventSession", "id", sessionId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Long eventId = session.getEvent().getId();

        // 1. Verify ticket ownership or elevated staff permissions
        boolean isStaff = user.getRoles().stream().anyMatch(r ->
                r.getName() == RoleName.ROLE_ADMIN || r.getName() == RoleName.ROLE_ORGANIZER);

        if (!isStaff && !ticketRepository.existsByOrderUserIdAndTicketTierEventId(userId, eventId)) {
            throw new BadRequestException("You must hold a valid confirmed ticket for '" +
                    session.getEvent().getTitle() + "' to reserve breakout session seats.");
        }

        // 2. Check if user already holds an RSVP for this session
        Optional<SessionRsvp> existingOpt = sessionRsvpRepository.findBySessionIdAndUserId(sessionId, userId);
        if (existingOpt.isPresent()) {
            SessionRsvp existing = existingOpt.get();
            if (existing.getStatus() == SessionRsvpStatus.CONFIRMED) {
                throw new BadRequestException("You already hold a confirmed reservation for '" + session.getTitle() + "'.");
            }
            if (existing.getStatus() == SessionRsvpStatus.WAITLISTED) {
                throw new BadRequestException("You are already on the waitlist for '" + session.getTitle() + "'.");
            }
        }

        // 3. Time Conflict Resolver: Check overlapping confirmed reservations in the same event
        List<SessionRsvp> confirmedRsvps = sessionRsvpRepository.findByUserIdAndEventIdAndStatus(
                userId, eventId, SessionRsvpStatus.CONFIRMED);

        for (SessionRsvp activeRsvp : confirmedRsvps) {
            EventSession other = activeRsvp.getSession();
            if (other.getId().equals(session.getId())) continue;

            // Overlap condition: startA < endB && endA > startB
            boolean overlaps = session.getStartTime().isBefore(other.getEndTime()) &&
                    session.getEndTime().isAfter(other.getStartTime());

            if (overlaps) {
                throw new BadRequestException("Schedule conflict: You already have a confirmed seat for '" +
                        other.getTitle() + "' in room " + (other.getRoom() != null ? other.getRoom() : "TBD") +
                        " (" + other.getStartTime().toLocalTime() + " - " + other.getEndTime().toLocalTime() +
                        "). Please release that reservation before booking this session.");
            }
        }

        // 4. Capacity & Waitlist determination
        int max = session.getMaxCapacity() != null && session.getMaxCapacity() > 0 ? session.getMaxCapacity() : 100;
        int reserved = session.getReservedSeats() != null ? session.getReservedSeats() : 0;

        SessionRsvpStatus status;
        Integer waitlistPos = null;

        if (reserved < max) {
            status = SessionRsvpStatus.CONFIRMED;
            session.setReservedSeats(reserved + 1);
            eventSessionRepository.save(session);
        } else {
            status = SessionRsvpStatus.WAITLISTED;
            long currentWaitlist = sessionRsvpRepository.countBySessionIdAndStatus(sessionId, SessionRsvpStatus.WAITLISTED);
            waitlistPos = (int) currentWaitlist + 1;
        }

        String rsvpCode = "RSVP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        SessionRsvp rsvp;

        if (existingOpt.isPresent()) {
            rsvp = existingOpt.get();
            rsvp.setStatus(status);
            rsvp.setRsvpCode(rsvpCode);
            rsvp.setReservedAt(java.time.LocalDateTime.now());
        } else {
            rsvp = new SessionRsvp(session, user, status, rsvpCode);
        }

        SessionRsvp saved = sessionRsvpRepository.save(rsvp);
        return mapToDto(saved, waitlistPos);
    }

    @Override
    public SessionRsvpDto cancelRsvp(Long sessionId, Long userId) {
        EventSession session = eventSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("EventSession", "id", sessionId));

        SessionRsvp rsvp = sessionRsvpRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BadRequestException("No reservation found for this session."));

        if (rsvp.getStatus() == SessionRsvpStatus.CANCELLED) {
            throw new BadRequestException("This reservation is already cancelled.");
        }

        SessionRsvpStatus previousStatus = rsvp.getStatus();
        rsvp.setStatus(SessionRsvpStatus.CANCELLED);
        SessionRsvp saved = sessionRsvpRepository.save(rsvp);

        if (previousStatus == SessionRsvpStatus.CONFIRMED) {
            int currentReserved = session.getReservedSeats() != null ? session.getReservedSeats() : 1;
            session.setReservedSeats(Math.max(0, currentReserved - 1));

            // Auto-promote first waitlisted attendee
            List<SessionRsvp> waitlist = sessionRsvpRepository
                    .findBySessionIdAndStatusOrderByReservedAtAsc(sessionId, SessionRsvpStatus.WAITLISTED);

            if (!waitlist.isEmpty()) {
                SessionRsvp promoted = waitlist.get(0);
                promoted.setStatus(SessionRsvpStatus.CONFIRMED);
                sessionRsvpRepository.save(promoted);

                session.setReservedSeats(session.getReservedSeats() + 1);

                if (notificationService != null) {
                    notificationService.sendNotification(
                            promoted.getUser(),
                            "Breakout Seat Confirmed!",
                            "A seat opened up for '" + session.getTitle() + "'. Your waitlist spot has been promoted to CONFIRMED!",
                            NotificationType.WAITLIST_OFFER,
                            promoted.getRsvpCode()
                    );
                }
            }
            eventSessionRepository.save(session);
        }

        return mapToDto(saved, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionRsvpDto> getMySchedule(Long eventId, Long userId) {
        List<SessionRsvp> rsvps = sessionRsvpRepository.findByUserIdAndStatus(userId, SessionRsvpStatus.CONFIRMED);
        List<SessionRsvp> waitlisted = sessionRsvpRepository.findByUserIdAndStatus(userId, SessionRsvpStatus.WAITLISTED);

        List<SessionRsvp> combined = new ArrayList<>(rsvps);
        combined.addAll(waitlisted);

        return combined.stream()
                .filter(r -> eventId == null || (r.getSession().getEvent() != null && r.getSession().getEvent().getId().equals(eventId)))
                .map(r -> {
                    Integer pos = null;
                    if (r.getStatus() == SessionRsvpStatus.WAITLISTED) {
                        List<SessionRsvp> queue = sessionRsvpRepository
                                .findBySessionIdAndStatusOrderByReservedAtAsc(r.getSession().getId(), SessionRsvpStatus.WAITLISTED);
                        for (int i = 0; i < queue.size(); i++) {
                            if (queue.get(i).getId().equals(r.getId())) {
                                pos = i + 1;
                                break;
                            }
                        }
                    }
                    return mapToDto(r, pos);
                })
                .sorted(Comparator.comparing(SessionRsvpDto::getStartTime))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SessionCapacityDto getSessionCapacity(Long sessionId) {
        EventSession session = eventSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("EventSession", "id", sessionId));

        return mapToCapacityDto(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomHeatmapDto> getRoomCapacityHeatmap(Long eventId) {
        List<EventSession> sessions = eventSessionRepository.findByEventIdOrderByStartTimeAsc(eventId);

        Map<String, List<EventSession>> byRoom = sessions.stream()
                .collect(Collectors.groupingBy(s -> s.getRoom() != null && !s.getRoom().isBlank() ? s.getRoom() : "General Hall"));

        List<RoomHeatmapDto> result = new ArrayList<>();

        for (Map.Entry<String, List<EventSession>> entry : byRoom.entrySet()) {
            String roomName = entry.getKey();
            List<EventSession> roomSessions = entry.getValue();

            String track = roomSessions.stream()
                    .map(EventSession::getTrack)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse("Main Conference");

            RoomHeatmapDto dto = new RoomHeatmapDto(roomName, track);
            dto.setSessionCount(roomSessions.size());

            int totCap = 0;
            int totRes = 0;
            double sumOccupancy = 0;

            List<SessionCapacityDto> capList = new ArrayList<>();
            for (EventSession s : roomSessions) {
                SessionCapacityDto cap = mapToCapacityDto(s);
                capList.add(cap);
                totCap += cap.getMaxCapacity();
                totRes += cap.getReservedSeats();
                sumOccupancy += cap.getOccupancyPercentage();
            }

            dto.setTotalCapacity(totCap);
            dto.setTotalReserved(totRes);
            dto.setAverageOccupancyPercentage(roomSessions.isEmpty() ? 0.0 :
                    Math.round((sumOccupancy / roomSessions.size()) * 10.0) / 10.0);
            dto.setSessions(capList);

            result.add(dto);
        }

        result.sort(Comparator.comparing(RoomHeatmapDto::getRoom));
        return result;
    }

    private SessionCapacityDto mapToCapacityDto(EventSession session) {
        SessionCapacityDto dto = new SessionCapacityDto();
        dto.setSessionId(session.getId());
        dto.setTitle(session.getTitle());
        dto.setRoom(session.getRoom());
        dto.setTrack(session.getTrack());
        dto.setStartTime(session.getStartTime());
        dto.setEndTime(session.getEndTime());

        int max = session.getMaxCapacity() != null && session.getMaxCapacity() > 0 ? session.getMaxCapacity() : 100;
        int res = session.getReservedSeats() != null ? session.getReservedSeats() : 0;
        int avail = Math.max(0, max - res);

        dto.setMaxCapacity(max);
        dto.setReservedSeats(res);
        dto.setAvailableSeats(avail);
        dto.setFull(res >= max);

        double pct = max > 0 ? ((double) res / max) * 100.0 : 0.0;
        dto.setOccupancyPercentage(Math.round(pct * 10.0) / 10.0);

        long waitlist = sessionRsvpRepository.countBySessionIdAndStatus(session.getId(), SessionRsvpStatus.WAITLISTED);
        dto.setWaitlistCount(waitlist);

        return dto;
    }

    private SessionRsvpDto mapToDto(SessionRsvp rsvp, Integer waitlistPos) {
        SessionRsvpDto dto = new SessionRsvpDto();
        dto.setId(rsvp.getId());
        dto.setSessionId(rsvp.getSession().getId());
        dto.setSessionTitle(rsvp.getSession().getTitle());
        dto.setRoom(rsvp.getSession().getRoom());
        dto.setTrack(rsvp.getSession().getTrack());
        dto.setStartTime(rsvp.getSession().getStartTime());
        dto.setEndTime(rsvp.getSession().getEndTime());
        if (rsvp.getSession().getSpeaker() != null) {
            dto.setSpeakerName(rsvp.getSession().getSpeaker().getName());
        }
        dto.setStatus(rsvp.getStatus());
        dto.setRsvpCode(rsvp.getRsvpCode());
        dto.setReservedAt(rsvp.getReservedAt());
        dto.setWaitlistPosition(waitlistPos);
        return dto;
    }
}
