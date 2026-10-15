package com.eventflow.repository;

import com.eventflow.entity.WaitlistEntry;
import com.eventflow.entity.enums.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    Optional<WaitlistEntry> findByTicketTierIdAndUserId(Long ticketTierId, Long userId);

    List<WaitlistEntry> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<WaitlistEntry> findByTicketTierIdAndStatusOrderByQueuePositionAsc(Long ticketTierId, WaitlistStatus status);

    List<WaitlistEntry> findByTicketTierIdOrderByQueuePositionAsc(Long ticketTierId);

    int countByTicketTierIdAndStatus(Long ticketTierId, WaitlistStatus status);

    boolean existsByTicketTierIdAndUserIdAndStatusIn(Long ticketTierId, Long userId, Collection<WaitlistStatus> statuses);

    List<WaitlistEntry> findByStatusAndOfferExpiresAtBefore(WaitlistStatus status, LocalDateTime now);

    @Query("SELECT COALESCE(MAX(w.queuePosition), 0) FROM WaitlistEntry w WHERE w.ticketTier.id = :tierId AND w.status = 'WAITING'")
    Integer findMaxQueuePositionByTierId(@Param("tierId") Long tierId);
}
