package com.eventflow.service.impl;

import com.eventflow.dto.*;
import com.eventflow.entity.*;
import com.eventflow.exception.BadRequestException;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.*;
import com.eventflow.service.AgendaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AgendaServiceImpl implements AgendaService {

    private final SpeakerRepository speakerRepository;
    private final EventSessionRepository eventSessionRepository;
    private final UserScheduleBookmarkRepository bookmarkRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter ICS_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    public AgendaServiceImpl(
            SpeakerRepository speakerRepository,
            EventSessionRepository eventSessionRepository,
            UserScheduleBookmarkRepository bookmarkRepository,
            EventRepository eventRepository,
            UserRepository userRepository) {
        this.speakerRepository = speakerRepository;
        this.eventSessionRepository = eventSessionRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public SpeakerDto createSpeaker(CreateSpeakerRequest request, Long currentUserId) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.getEventId()));

        Speaker speaker = new Speaker(
                event,
                request.getName(),
                request.getTitle(),
                request.getCompany(),
                request.getBio(),
                request.getAvatarUrl(),
                request.getSocialUrl()
        );

        Speaker saved = speakerRepository.save(speaker);
        return mapToSpeakerDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpeakerDto> getSpeakersByEvent(Long eventId) {
        return speakerRepository.findByEventIdOrderByNameAsc(eventId).stream()
                .map(this::mapToSpeakerDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSpeaker(Long speakerId, Long currentUserId) {
        Speaker speaker = speakerRepository.findById(speakerId)
                .orElseThrow(() -> new ResourceNotFoundException("Speaker not found: " + speakerId));
        speakerRepository.delete(speaker);
    }

    @Override
    @Transactional
    public EventSessionDto createSession(CreateSessionRequest request, Long currentUserId) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.getEventId()));

        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new BadRequestException("Session end time must be after start time");
        }

        Speaker speaker = null;
        if (request.getSpeakerId() != null) {
            speaker = speakerRepository.findById(request.getSpeakerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Speaker not found: " + request.getSpeakerId()));
        }

        EventSession session = new EventSession(
                event,
                speaker,
                request.getTitle(),
                request.getDescription(),
                request.getRoom(),
                request.getTrack(),
                request.getSessionType(),
                request.getStartTime(),
                request.getEndTime(),
                request.getMaxCapacity() != null ? request.getMaxCapacity() : 100
        );

        EventSession saved = eventSessionRepository.save(session);
        return mapToSessionDto(saved, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSessionDto> getSessionsByEvent(Long eventId, Long currentUserId) {
        List<EventSession> sessions = eventSessionRepository.findByEventIdOrderByStartTimeAsc(eventId);

        Set<Long> bookmarkedSessionIds = Collections.emptySet();
        if (currentUserId != null) {
            bookmarkedSessionIds = bookmarkRepository.findByUserIdAndSessionEventId(currentUserId, eventId).stream()
                    .map(b -> b.getSession().getId())
                    .collect(Collectors.toSet());
        }

        final Set<Long> finalBookmarked = bookmarkedSessionIds;
        return sessions.stream()
                .map(s -> mapToSessionDto(s, finalBookmarked.contains(s.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId, Long currentUserId) {
        EventSession session = eventSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        eventSessionRepository.delete(session);
    }

    @Override
    @Transactional
    public BookmarkToggleResponse toggleSessionBookmark(Long userId, Long sessionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        EventSession session = eventSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        Optional<UserScheduleBookmark> existing = bookmarkRepository.findByUserIdAndSessionId(userId, sessionId);
        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
            return new BookmarkToggleResponse(sessionId, false, "Session removed from personal schedule");
        } else {
            UserScheduleBookmark bookmark = new UserScheduleBookmark(user, session);
            bookmarkRepository.save(bookmark);
            return new BookmarkToggleResponse(sessionId, true, "Session added to personal schedule");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSessionDto> getMyBookmarkedSessions(Long userId, Long eventId) {
        List<UserScheduleBookmark> bookmarks;
        if (eventId != null) {
            bookmarks = bookmarkRepository.findByUserIdAndSessionEventId(userId, eventId);
        } else {
            bookmarks = bookmarkRepository.findByUserId(userId);
        }

        return bookmarks.stream()
                .map(b -> mapToSessionDto(b.getSession(), true))
                .sorted(Comparator.comparing(EventSessionDto::getStartTime))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public String generateIcsCalendar(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        List<EventSession> sessions;
        if (userId != null && bookmarkRepository.findByUserIdAndSessionEventId(userId, eventId).size() > 0) {
            sessions = bookmarkRepository.findByUserIdAndSessionEventId(userId, eventId).stream()
                    .map(UserScheduleBookmark::getSession)
                    .sorted(Comparator.comparing(EventSession::getStartTime))
                    .collect(Collectors.toList());
        } else {
            sessions = eventSessionRepository.findByEventIdOrderByStartTimeAsc(eventId);
        }

        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR\r\n");
        ics.append("VERSION:2.0\r\n");
        ics.append("PRODID:-//PulseEvent OS//Schedule Engine 1.0//EN\r\n");
        ics.append("CALSCALE:GREGORIAN\r\n");
        ics.append("METHOD:PUBLISH\r\n");
        ics.append("X-WR-CALNAME:").append(escapeIcs(event.getTitle())).append(" Schedule\r\n");
        ics.append("X-WR-TIMEZONE:UTC\r\n");

        for (EventSession s : sessions) {
            ics.append("BEGIN:VEVENT\r\n");
            ics.append("UID:session-").append(s.getId()).append("-").append(eventId).append("@pulsevent.io\r\n");
            ics.append("DTSTAMP:").append(s.getCreatedAt().atZone(ZoneOffset.UTC).format(ICS_DATE_TIME)).append("\r\n");
            ics.append("DTSTART:").append(s.getStartTime().atZone(ZoneOffset.UTC).format(ICS_DATE_TIME)).append("\r\n");
            ics.append("DTEND:").append(s.getEndTime().atZone(ZoneOffset.UTC).format(ICS_DATE_TIME)).append("\r\n");
            ics.append("SUMMARY:").append(escapeIcs(s.getTitle())).append("\r\n");

            StringBuilder desc = new StringBuilder();
            if (s.getDescription() != null) desc.append(s.getDescription()).append(" ");
            if (s.getSpeaker() != null) desc.append("[Speaker: ").append(s.getSpeaker().getName()).append("] ");
            if (s.getTrack() != null) desc.append("[Track: ").append(s.getTrack()).append("]");

            ics.append("DESCRIPTION:").append(escapeIcs(desc.toString().trim())).append("\r\n");
            if (s.getRoom() != null) {
                ics.append("LOCATION:").append(escapeIcs(s.getRoom())).append("\r\n");
            }
            ics.append("STATUS:CONFIRMED\r\n");
            ics.append("END:VEVENT\r\n");
        }

        ics.append("END:VCALENDAR\r\n");
        return ics.toString();
    }

    private String escapeIcs(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace(";", "\\;")
                   .replace(",", "\\,")
                   .replace("\n", "\\n")
                   .replace("\r", "");
    }

    private SpeakerDto mapToSpeakerDto(Speaker s) {
        return new SpeakerDto(
                s.getId(),
                s.getEvent().getId(),
                s.getName(),
                s.getTitle(),
                s.getCompany(),
                s.getBio(),
                s.getAvatarUrl(),
                s.getSocialUrl(),
                s.getCreatedAt()
        );
    }

    private EventSessionDto mapToSessionDto(EventSession s, boolean isBookmarked) {
        EventSessionDto dto = new EventSessionDto();
        dto.setId(s.getId());
        dto.setEventId(s.getEvent().getId());
        dto.setEventTitle(s.getEvent().getTitle());
        dto.setTitle(s.getTitle());
        dto.setDescription(s.getDescription());
        dto.setRoom(s.getRoom());
        dto.setTrack(s.getTrack());
        dto.setSessionType(s.getSessionType());
        dto.setStartTime(s.getStartTime());
        dto.setEndTime(s.getEndTime());
        dto.setBookmarked(isBookmarked);

        int max = s.getMaxCapacity() != null && s.getMaxCapacity() > 0 ? s.getMaxCapacity() : 100;
        int res = s.getReservedSeats() != null ? s.getReservedSeats() : 0;
        dto.setMaxCapacity(max);
        dto.setReservedSeats(res);
        dto.setAvailableSeats(Math.max(0, max - res));

        if (s.getSpeaker() != null) {
            dto.setSpeaker(mapToSpeakerDto(s.getSpeaker()));
        }
        return dto;
    }
}
