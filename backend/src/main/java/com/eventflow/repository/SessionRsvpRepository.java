package com.eventflow.repository;

import com.eventflow.entity.SessionRsvp;
import com.eventflow.entity.enums.SessionRsvpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRsvpRepository extends JpaRepository<SessionRsvp, Long> {

    Optional<SessionRsvp> findBySessionIdAndUserId(Long sessionId, Long userId);

    Optional<SessionRsvp> findByRsvpCode(String rsvpCode);

    List<SessionRsvp> findByUserIdAndStatus(Long userId, SessionRsvpStatus status);

    @Query("SELECT r FROM SessionRsvp r WHERE r.user.id = :userId AND r.session.event.id = :eventId AND r.status = :status")
    List<SessionRsvp> findByUserIdAndEventIdAndStatus(
            @Param("userId") Long userId,
            @Param("eventId") Long eventId,
            @Param("status") SessionRsvpStatus status
    );

    List<SessionRsvp> findBySessionIdAndStatusOrderByReservedAtAsc(Long sessionId, SessionRsvpStatus status);

    long countBySessionIdAndStatus(Long sessionId, SessionRsvpStatus status);

    boolean existsBySessionIdAndUserIdAndStatus(Long sessionId, Long userId, SessionRsvpStatus status);
}
