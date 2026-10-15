package com.eventflow.repository;

import com.eventflow.entity.VenueSeatSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueSeatSectionRepository extends JpaRepository<VenueSeatSection, Long> {
    List<VenueSeatSection> findByVenueIdOrderByNameAsc(Long venueId);
}
