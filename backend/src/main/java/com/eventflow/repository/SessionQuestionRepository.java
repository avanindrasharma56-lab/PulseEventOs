package com.eventflow.repository;

import com.eventflow.entity.SessionQuestion;
import com.eventflow.entity.enums.QuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionQuestionRepository extends JpaRepository<SessionQuestion, Long> {

    List<SessionQuestion> findByEventIdOrderByUpvotesDescCreatedAtDesc(Long eventId);

    List<SessionQuestion> findByEventIdAndStatusNotOrderByUpvotesDescCreatedAtDesc(Long eventId, QuestionStatus status);

    List<SessionQuestion> findByEventIdAndStatusOrderByUpvotesDescCreatedAtDesc(Long eventId, QuestionStatus status);
}
