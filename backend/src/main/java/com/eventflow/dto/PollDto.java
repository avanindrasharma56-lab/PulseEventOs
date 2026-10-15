package com.eventflow.dto;

import com.eventflow.entity.enums.PollStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PollDto {

    private Long id;
    private Long eventId;
    private Long sessionId;
    private String question;
    private PollStatus status;
    private Integer totalVotes;
    private Long userVotedOptionId;
    private List<PollOptionDto> options = new ArrayList<>();
    private LocalDateTime createdAt;

    public PollDto() {
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

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public PollStatus getStatus() {
        return status;
    }

    public void setStatus(PollStatus status) {
        this.status = status;
    }

    public Integer getTotalVotes() {
        return totalVotes;
    }

    public void setTotalVotes(Integer totalVotes) {
        this.totalVotes = totalVotes;
    }

    public Long getUserVotedOptionId() {
        return userVotedOptionId;
    }

    public void setUserVotedOptionId(Long userVotedOptionId) {
        this.userVotedOptionId = userVotedOptionId;
    }

    public boolean isHasVoted() {
        return userVotedOptionId != null;
    }

    public List<PollOptionDto> getOptions() {
        return options;
    }

    public void setOptions(List<PollOptionDto> options) {
        this.options = options;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
