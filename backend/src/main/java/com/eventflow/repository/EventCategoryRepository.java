package com.eventflow.repository;

import com.eventflow.entity.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventCategoryRepository extends JpaRepository<EventCategory, Long> {
    Optional<EventCategory> findBySlug(String slug);
    List<EventCategory> findByActiveTrueOrderByNameAsc();
    boolean existsByName(String name);
    boolean existsBySlug(String slug);
}
