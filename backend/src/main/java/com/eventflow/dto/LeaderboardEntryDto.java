package com.eventflow.dto;

import com.eventflow.entity.GamificationTier;

public class LeaderboardEntryDto {
    private Integer rank;
    private Long userId;
    private String userName;
    private String userEmail;
    private Integer totalXp;
    private GamificationTier currentTier;
    private Integer badgesCount;
    private String headline;
    private String company;

    public LeaderboardEntryDto() {
    }

    public LeaderboardEntryDto(Integer rank, Long userId, String userName, String userEmail,
                               Integer totalXp, GamificationTier currentTier, Integer badgesCount,
                               String headline, String company) {
        this.rank = rank;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.totalXp = totalXp;
        this.currentTier = currentTier;
        this.badgesCount = badgesCount;
        this.headline = headline;
        this.company = company;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
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

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
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

    public Integer getBadgesCount() {
        return badgesCount;
    }

    public void setBadgesCount(Integer badgesCount) {
        this.badgesCount = badgesCount;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }
}
