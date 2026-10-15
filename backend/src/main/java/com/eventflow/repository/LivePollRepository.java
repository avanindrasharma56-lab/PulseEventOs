package com.eventflow.repository;

import com.eventflow.entity.LivePoll;
import com.eventflow.entity.enums.PollStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LivePollRepository extends JpaRepository<LivePoll, Long> {

    List<LivePoll> findByEventIdOrderByCreatedAtDesc(Long eventId);

    List<LivePoll> findByEventIdAndStatusOrderByCreatedAtDesc(Long eventId, PollStatus status);
}
