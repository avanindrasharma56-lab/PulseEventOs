package com.eventflow.dto;

import java.time.LocalDateTime;

public class AttendeeBadgeDto {
    private Long id;
    private String badgeKey;
    private String badgeName;
    private String badgeIcon;
    private LocalDateTime awardedAt;

    public AttendeeBadgeDto() {
    }

    public AttendeeBadgeDto(Long id, String badgeKey, String badgeName, String badgeIcon, LocalDateTime awardedAt) {
        this.id = id;
        this.badgeKey = badgeKey;
        this.badgeName = badgeName;
        this.badgeIcon = badgeIcon;
        this.awardedAt = awardedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBadgeKey() {
        return badgeKey;
    }

    public void setBadgeKey(String badgeKey) {
        this.badgeKey = badgeKey;
    }

    public String getBadgeName() {
        return badgeName;
    }

    public void setBadgeName(String badgeName) {
        this.badgeName = badgeName;
    }

    public String getBadgeIcon() {
        return badgeIcon;
    }

    public void setBadgeIcon(String badgeIcon) {
        this.badgeIcon = badgeIcon;
    }

    public LocalDateTime getAwardedAt() {
        return awardedAt;
    }

    public void setAwardedAt(LocalDateTime awardedAt) {
        this.awardedAt = awardedAt;
    }
}
