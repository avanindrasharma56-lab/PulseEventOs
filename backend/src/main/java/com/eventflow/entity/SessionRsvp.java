package com.eventflow.entity;

import com.eventflow.entity.enums.SessionRsvpStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_rsvps", uniqueConstraints = {
    @UniqueConstraint(name = "uk_session_user_rsvp", columnNames = {"session_id", "user_id"}),
    @UniqueConstraint(name = "uk_rsvp_code", columnNames = {"rsvp_code"})
})
public class SessionRsvp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private EventSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SessionRsvpStatus status = SessionRsvpStatus.CONFIRMED;

    @Column(name = "rsvp_code", nullable = false, length = 40, unique = true)
    private String rsvpCode;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt = LocalDateTime.now();

    public SessionRsvp() {
    }

    public SessionRsvp(EventSession session, User user, SessionRsvpStatus status, String rsvpCode) {
        this.session = session;
        this.user = user;
        this.status = status != null ? status : SessionRsvpStatus.CONFIRMED;
        this.rsvpCode = rsvpCode;
        this.reservedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EventSession getSession() {
        return session;
    }

    public void setSession(EventSession session) {
        this.session = session;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public SessionRsvpStatus getStatus() {
        return status;
    }

    public void setStatus(SessionRsvpStatus status) {
        this.status = status;
    }

    public String getRsvpCode() {
        return rsvpCode;
    }

    public void setRsvpCode(String rsvpCode) {
        this.rsvpCode = rsvpCode;
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(LocalDateTime reservedAt) {
        this.reservedAt = reservedAt;
    }
}
