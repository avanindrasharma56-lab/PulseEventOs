package com.eventflow.repository;

import com.eventflow.entity.Sponsor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SponsorRepository extends JpaRepository<Sponsor, Long> {
    List<Sponsor> findByEvent_IdOrderByDisplayOrderAsc(Long eventId);
}
