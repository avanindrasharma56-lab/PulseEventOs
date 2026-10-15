package com.eventflow.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "question_upvotes")
@IdClass(QuestionUpvote.QuestionUpvoteId.class)
public class QuestionUpvote {

    @Id
    @Column(name = "question_id")
    private Long questionId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public QuestionUpvote() {
    }

    public QuestionUpvote(Long questionId, Long userId) {
        this.questionId = questionId;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static class QuestionUpvoteId implements Serializable {
        private Long questionId;
        private Long userId;

        public QuestionUpvoteId() {}

        public QuestionUpvoteId(Long questionId, Long userId) {
            this.questionId = questionId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QuestionUpvoteId that = (QuestionUpvoteId) o;
            return Objects.equals(questionId, that.questionId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(questionId, userId);
        }
    }
}
