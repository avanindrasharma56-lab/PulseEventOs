package com.eventflow.repository;

import com.eventflow.entity.EventSeatReservation;
import com.eventflow.entity.enums.SeatReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventSeatReservationRepository extends JpaRepository<EventSeatReservation, Long> {
    List<EventSeatReservation> findByEventId(Long eventId);
    Optional<EventSeatReservation> findByEventIdAndSeatId(Long eventId, Long seatId);
    List<EventSeatReservation> findByEventIdAndUserIdAndStatus(Long eventId, Long userId, SeatReservationStatus status);
    List<EventSeatReservation> findByStatusAndExpiresAtBefore(SeatReservationStatus status, LocalDateTime now);

    @Query("SELECT r FROM EventSeatReservation r WHERE r.event.id = :eventId AND (r.status = 'RESERVED' OR (r.status = 'HOLD' AND r.expiresAt > :now))")
    List<EventSeatReservation> findActiveReservationsForEvent(@Param("eventId") Long eventId, @Param("now") LocalDateTime now);
}
