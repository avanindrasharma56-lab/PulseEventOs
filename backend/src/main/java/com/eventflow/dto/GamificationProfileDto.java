package com.eventflow.dto;

import com.eventflow.entity.GamificationTier;
import java.util.ArrayList;
import java.util.List;

public class GamificationProfileDto {
    private Long id;
    private Long eventId;
    private Long userId;
    private String userName;
    private Integer totalXp;
    private GamificationTier currentTier;
    private Integer rankPosition;
    private Integer nextTierMinXp;
    private String swagCode;
    private Boolean isSwagClaimed;
    private List<AttendeeBadgeDto> badges = new ArrayList<>();
    private List<GamificationActivityLogDto> recentActivities = new ArrayList<>();

    public GamificationProfileDto() {
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(Integer totalXp) {
        this.totalXp = totalXp;
    }

    public GamificationTier getCurrentTier() {
        return currentTier;
    }

    public void setCurrentTier(GamificationTier currentTier) {
        this.currentTier = currentTier;
    }

    public Integer getRankPosition() {
        return rankPosition;
    }

    public void setRankPosition(Integer rankPosition) {
        this.rankPosition = rankPosition;
    }

    public Integer getNextTierMinXp() {
        return nextTierMinXp;
    }

    public void setNextTierMinXp(Integer nextTierMinXp) {
        this.nextTierMinXp = nextTierMinXp;
    }

    public String getSwagCode() {
        return swagCode;
    }

    public void setSwagCode(String swagCode) {
        this.swagCode = swagCode;
    }

    public Boolean getIsSwagClaimed() {
        return isSwagClaimed;
    }

    public void setIsSwagClaimed(Boolean isSwagClaimed) {
        this.isSwagClaimed = isSwagClaimed;
    }

    public List<AttendeeBadgeDto> getBadges() {
        return badges;
    }

    public void setBadges(List<AttendeeBadgeDto> badges) {
        this.badges = badges;
    }

    public List<GamificationActivityLogDto> getRecentActivities() {
        return recentActivities;
    }

    public void setRecentActivities(List<GamificationActivityLogDto> recentActivities) {
        this.recentActivities = recentActivities;
    }
}
