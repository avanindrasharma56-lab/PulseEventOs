package com.eventflow.repository;

import com.eventflow.entity.AttendeeBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendeeBadgeRepository extends JpaRepository<AttendeeBadge, Long> {

    List<AttendeeBadge> findByProfileIdOrderByAwardedAtAsc(Long profileId);

    boolean existsByProfileIdAndBadgeKey(Long profileId, String badgeKey);
}
