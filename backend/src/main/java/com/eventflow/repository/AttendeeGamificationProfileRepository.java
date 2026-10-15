package com.eventflow.repository;

import com.eventflow.entity.AttendeeGamificationProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendeeGamificationProfileRepository extends JpaRepository<AttendeeGamificationProfile, Long> {

    Optional<AttendeeGamificationProfile> findByEventIdAndUserId(Long eventId, Long userId);

    @Query("SELECT p FROM AttendeeGamificationProfile p WHERE p.event.id = :eventId ORDER BY p.totalXp DESC, p.updatedAt ASC")
    List<AttendeeGamificationProfile> findTopLeaderboardByEventId(@Param("eventId") Long eventId, Pageable pageable);

    @Query("SELECT COUNT(p) + 1 FROM AttendeeGamificationProfile p WHERE p.event.id = :eventId AND p.totalXp > :xp")
    long calculateRank(@Param("eventId") Long eventId, @Param("xp") Integer xp);
}
