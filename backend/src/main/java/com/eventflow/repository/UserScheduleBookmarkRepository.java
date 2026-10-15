package com.eventflow.repository;

import com.eventflow.entity.UserScheduleBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserScheduleBookmarkRepository extends JpaRepository<UserScheduleBookmark, Long> {
    List<UserScheduleBookmark> findByUserId(Long userId);
    List<UserScheduleBookmark> findByUserIdAndSessionEventId(Long userId, Long eventId);
    Optional<UserScheduleBookmark> findByUserIdAndSessionId(Long userId, Long sessionId);
    boolean existsByUserIdAndSessionId(Long userId, Long sessionId);
    void deleteByUserIdAndSessionId(Long userId, Long sessionId);
}
