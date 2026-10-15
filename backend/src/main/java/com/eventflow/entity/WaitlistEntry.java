package com.eventflow.entity;

import com.eventflow.entity.enums.WaitlistStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "waitlist_entries", uniqueConstraints = {
    @UniqueConstraint(name = "uk_waitlist_tier_user", columnNames = {"ticket_tier_id", "user_id"})
})
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_tier_id", nullable = false)
    private TicketTier ticketTier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private WaitlistStatus status = WaitlistStatus.WAITING;

    @Column(name = "queue_position", nullable = false)
    private int queuePosition = 1;

    @Column(name = "offer_expires_at")
    private LocalDateTime offerExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public WaitlistEntry() {}

    public WaitlistEntry(TicketTier ticketTier, User user, int queuePosition) {
        this.ticketTier = ticketTier;
        this.user = user;
        this.queuePosition = queuePosition;
        this.status = WaitlistStatus.WAITING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TicketTier getTicketTier() {
        return ticketTier;
    }

    public void setTicketTier(TicketTier ticketTier) {
        this.ticketTier = ticketTier;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public WaitlistStatus getStatus() {
        return status;
    }

    public void setStatus(WaitlistStatus status) {
        this.status = status;
    }

    public int getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(int queuePosition) {
        this.queuePosition = queuePosition;
    }

    public LocalDateTime getOfferExpiresAt() {
        return offerExpiresAt;
    }

    public void setOfferExpiresAt(LocalDateTime offerExpiresAt) {
        this.offerExpiresAt = offerExpiresAt;
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
