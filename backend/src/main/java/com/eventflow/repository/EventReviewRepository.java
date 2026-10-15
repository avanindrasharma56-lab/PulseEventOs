package com.eventflow.repository;

import com.eventflow.entity.EventReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventReviewRepository extends JpaRepository<EventReview, Long> {
    List<EventReview> findByEventIdOrderByCreatedAtDesc(Long eventId);
    Optional<EventReview> findByUserIdAndEventId(Long userId, Long eventId);
    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    @Query("SELECT AVG(r.rating) FROM EventReview r WHERE r.event.id = :eventId")
    Double getAverageRatingByEventId(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(r) FROM EventReview r WHERE r.event.id = :eventId")
    Long countByEventId(@Param("eventId") Long eventId);
}
