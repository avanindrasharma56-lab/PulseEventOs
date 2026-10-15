package com.eventflow.repository;

import com.eventflow.entity.QuestionUpvote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionUpvoteRepository extends JpaRepository<QuestionUpvote, QuestionUpvote.QuestionUpvoteId> {

    boolean existsByQuestionIdAndUserId(Long questionId, Long userId);

    Optional<QuestionUpvote> findByQuestionIdAndUserId(Long questionId, Long userId);

    void deleteByQuestionIdAndUserId(Long questionId, Long userId);

    long countByQuestionId(Long questionId);
}
