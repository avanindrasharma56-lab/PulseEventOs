package com.eventflow.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "gamification_activity_logs", uniqueConstraints = {
    @UniqueConstraint(name = "uk_gamify_log_activity", columnNames = {"profile_id", "activity_type", "reference_id"})
})
public class GamificationActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private AttendeeGamificationProfile profile;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private GamificationActivityType activityType;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "xp_earned", nullable = false)
    private Integer xpEarned;

    @Column(nullable = false, length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public GamificationActivityLog() {
    }

    public GamificationActivityLog(AttendeeGamificationProfile profile, GamificationActivityType activityType,
                                   String referenceId, Integer xpEarned, String description) {
        this.profile = profile;
        this.activityType = activityType;
        this.referenceId = referenceId;
        this.xpEarned = xpEarned;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AttendeeGamificationProfile getProfile() {
        return profile;
    }

    public void setProfile(AttendeeGamificationProfile profile) {
        this.profile = profile;
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
