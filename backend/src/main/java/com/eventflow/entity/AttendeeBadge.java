package com.eventflow.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendee_badges", uniqueConstraints = {
    @UniqueConstraint(name = "uk_badge_profile_key", columnNames = {"profile_id", "badge_key"})
})
public class AttendeeBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private AttendeeGamificationProfile profile;

    @Column(name = "badge_key", nullable = false, length = 50)
    private String badgeKey;

    @Column(name = "badge_name", nullable = false, length = 100)
    private String badgeName;

    @Column(name = "badge_icon", nullable = false, length = 50)
    private String badgeIcon;

    @CreationTimestamp
    @Column(name = "awarded_at", nullable = false, updatable = false)
    private LocalDateTime awardedAt;

    public AttendeeBadge() {
    }

    public AttendeeBadge(AttendeeGamificationProfile profile, String badgeKey, String badgeName, String badgeIcon) {
        this.profile = profile;
        this.badgeKey = badgeKey;
        this.badgeName = badgeName;
        this.badgeIcon = badgeIcon;
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
