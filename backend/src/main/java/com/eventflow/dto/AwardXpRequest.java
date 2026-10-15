package com.eventflow.dto;

import com.eventflow.entity.GamificationActivityType;

public class AwardXpRequest {
    private GamificationActivityType activityType;
    private String referenceId;
    private String description;
    private Integer customXp;

    public AwardXpRequest() {
    }

    public AwardXpRequest(GamificationActivityType activityType, String referenceId, String description, Integer customXp) {
        this.activityType = activityType;
        this.referenceId = referenceId;
        this.description = description;
        this.customXp = customXp;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCustomXp() {
        return customXp;
    }

    public void setCustomXp(Integer customXp) {
        this.customXp = customXp;
    }
}
