package com.eventflow.dto;

import com.eventflow.entity.GamificationActivityType;
import java.time.LocalDateTime;

public class GamificationActivityLogDto {
    private Long id;
    private GamificationActivityType activityType;
    private String referenceId;
    private Integer xpEarned;
    private String description;
    private LocalDateTime createdAt;

    public GamificationActivityLogDto() {
    }

    public GamificationActivityLogDto(Long id, GamificationActivityType activityType, String referenceId,
                                      Integer xpEarned, String description, LocalDateTime createdAt) {
        this.id = id;
        this.activityType = activityType;
        this.referenceId = referenceId;
        this.xpEarned = xpEarned;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GamificationActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(GamificationActivityType activityType) {
        this.activityType = activityType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public Integer getXpEarned() {
        return xpEarned;
    }

    public void setXpEarned(Integer xpEarned) {
        this.xpEarned = xpEarned;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
