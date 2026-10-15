package com.eventflow.repository;

import com.eventflow.entity.LeadRetrieval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeadRetrievalRepository extends JpaRepository<LeadRetrieval, Long> {
    List<LeadRetrieval> findBySponsor_IdOrderByScannedAtDesc(Long sponsorId);
    Optional<LeadRetrieval> findBySponsor_IdAndAttendee_Id(Long sponsorId, Long attendeeId);
}
