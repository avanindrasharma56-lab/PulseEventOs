package com.eventflow.repository;

import com.eventflow.entity.Event;
import com.eventflow.entity.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Event> findByOrganizerIdOrderByCreatedAtDesc(Long organizerId);

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    long countByStatus(EventStatus status);

    @Query("""
        SELECT e FROM Event e
        LEFT JOIN e.venue v
        WHERE e.status = :status
        AND (:categoryId IS NULL OR e.category.id = :categoryId)
        AND (:search IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.shortSummary) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:isOnline IS NULL OR (v IS NOT NULL AND v.online = :isOnline))
        AND (:city IS NULL OR (v IS NOT NULL AND LOWER(v.city) = LOWER(:city)))
        AND (:startDate IS NULL OR e.startTime >= :startDate)
        AND (:endDate IS NULL OR e.endTime <= :endDate)
        ORDER BY e.featured DESC, e.startTime ASC
    """)
    Page<Event> searchPublishedEvents(
            @Param("status") EventStatus status,
            @Param("categoryId") Long categoryId,
            @Param("search") String search,
            @Param("isOnline") Boolean isOnline,
            @Param("city") String city,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
