package com.eventflow.repository;

import com.eventflow.entity.GamificationActivityLog;
import com.eventflow.entity.GamificationActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GamificationActivityLogRepository extends JpaRepository<GamificationActivityLog, Long> {

    boolean existsByProfileIdAndActivityTypeAndReferenceId(Long profileId, GamificationActivityType activityType, String referenceId);

    List<GamificationActivityLog> findByProfileIdOrderByCreatedAtDesc(Long profileId);
}
