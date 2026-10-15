package com.eventflow.repository;

import com.eventflow.entity.VenueSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VenueSeatRepository extends JpaRepository<VenueSeat, Long> {
    List<VenueSeat> findBySectionIdOrderByRowLabelAscSeatNumberAsc(Long sectionId);
    List<VenueSeat> findBySectionVenueId(Long venueId);
    Optional<VenueSeat> findBySectionIdAndRowLabelAndSeatNumber(Long sectionId, String rowLabel, Integer seatNumber);
}
