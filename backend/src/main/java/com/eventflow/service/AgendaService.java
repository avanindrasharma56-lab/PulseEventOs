package com.eventflow.service;

import com.eventflow.dto.*;
import java.util.List;

public interface AgendaService {
    SpeakerDto createSpeaker(CreateSpeakerRequest request, Long currentUserId);
    List<SpeakerDto> getSpeakersByEvent(Long eventId);
    void deleteSpeaker(Long speakerId, Long currentUserId);

    EventSessionDto createSession(CreateSessionRequest request, Long currentUserId);
    List<EventSessionDto> getSessionsByEvent(Long eventId, Long currentUserId);
    void deleteSession(Long sessionId, Long currentUserId);

    BookmarkToggleResponse toggleSessionBookmark(Long userId, Long sessionId);
    List<EventSessionDto> getMyBookmarkedSessions(Long userId, Long eventId);

    String generateIcsCalendar(Long eventId, Long userId);
}
