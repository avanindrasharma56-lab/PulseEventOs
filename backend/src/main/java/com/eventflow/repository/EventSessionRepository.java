package com.eventflow.repository;

import com.eventflow.entity.EventSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventSessionRepository extends JpaRepository<EventSession, Long> {
    List<EventSession> findByEventIdOrderByStartTimeAsc(Long eventId);
    List<EventSession> findByEventIdAndTrackOrderByStartTimeAsc(Long eventId, String track);
    List<EventSession> findBySpeakerId(Long speakerId);
}
