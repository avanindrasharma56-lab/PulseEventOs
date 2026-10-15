package com.eventflow.dto;

import com.eventflow.entity.enums.QuestionStatus;
import java.time.LocalDateTime;

public class QuestionDto {

    private Long id;
    private Long eventId;
    private Long sessionId;
    private String authorName;
    private String questionText;
    private Integer upvotes;
    private boolean anonymous;
    private QuestionStatus status;
    private boolean userHasUpvoted;
    private LocalDateTime createdAt;

    public QuestionDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public Integer getUpvotes() {
        return upvotes;
    }

    public void setUpvotes(Integer upvotes) {
        this.upvotes = upvotes;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionStatus status) {
        this.status = status;
    }

    public boolean isUserHasUpvoted() {
        return userHasUpvoted;
    }

    public void setUserHasUpvoted(boolean userHasUpvoted) {
        this.userHasUpvoted = userHasUpvoted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
