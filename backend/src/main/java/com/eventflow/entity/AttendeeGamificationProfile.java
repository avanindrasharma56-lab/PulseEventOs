package com.eventflow.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendee_gamification_profiles", uniqueConstraints = {
    @UniqueConstraint(name = "uk_gamify_user_event", columnNames = {"event_id", "user_id"})
})
public class AttendeeGamificationProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_xp", nullable = false)
    private Integer totalXp = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_tier", nullable = false, length = 30)
    private GamificationTier currentTier = GamificationTier.NOVICE;

    @Column(name = "rank_position")
    private Integer rankPosition;

    @Column(name = "swag_code", length = 50)
    private String swagCode;

    @Column(name = "is_swag_claimed", nullable = false)
    private Boolean isSwagClaimed = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public AttendeeGamificationProfile() {
    }

    public AttendeeGamificationProfile(Event event, User user) {
        this.event = event;
        this.user = user;
        this.totalXp = 0;
        this.currentTier = GamificationTier.NOVICE;
        this.isSwagClaimed = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
