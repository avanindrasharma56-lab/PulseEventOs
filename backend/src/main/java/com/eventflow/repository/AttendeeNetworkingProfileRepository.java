package com.eventflow.repository;

import com.eventflow.entity.AttendeeNetworkingProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendeeNetworkingProfileRepository extends JpaRepository<AttendeeNetworkingProfile, Long> {

    Optional<AttendeeNetworkingProfile> findByEventIdAndUserId(Long eventId, Long userId);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    List<AttendeeNetworkingProfile> findByEventIdAndIsAvailableTrueOrderByCreatedAtDesc(Long eventId);

    @Query("SELECT p FROM AttendeeNetworkingProfile p WHERE p.event.id = :eventId " +
           "AND p.isAvailable = true " +
           "AND (:query IS NULL OR :query = '' OR " +
           "     LOWER(p.headline) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(p.company) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(p.jobTitle) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(p.interests) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(p.user.fullName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:interest IS NULL OR :interest = '' OR LOWER(p.interests) LIKE LOWER(CONCAT('%', :interest, '%'))) " +
           "ORDER BY p.createdAt DESC")
    List<AttendeeNetworkingProfile> searchProfiles(@Param("eventId") Long eventId,
                                                   @Param("query") String query,
                                                   @Param("interest") String interest);
}
