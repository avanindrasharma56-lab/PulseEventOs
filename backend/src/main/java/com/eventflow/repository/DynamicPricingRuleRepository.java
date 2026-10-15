package com.eventflow.repository;

import com.eventflow.entity.DynamicPricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DynamicPricingRuleRepository extends JpaRepository<DynamicPricingRule, Long> {
    @Query("SELECT r FROM DynamicPricingRule r WHERE r.event.id = :eventId AND r.active = true")
    List<DynamicPricingRule> findByEventIdAndActiveTrue(@Param("eventId") Long eventId);
}
