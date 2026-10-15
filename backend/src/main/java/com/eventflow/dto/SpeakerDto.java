package com.eventflow.dto;

import java.time.LocalDateTime;

public class SpeakerDto {
    private Long id;
    private Long eventId;
    private String name;
    private String title;
    private String company;
    private String bio;
    private String avatarUrl;
    private String socialUrl;
    private LocalDateTime createdAt;

    public SpeakerDto() {}

    public SpeakerDto(Long id, Long eventId, String name, String title, String company, String bio, String avatarUrl, String socialUrl, LocalDateTime createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.name = name;
        this.title = title;
        this.company = company;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
        this.socialUrl = socialUrl;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getSocialUrl() { return socialUrl; }
    public void setSocialUrl(String socialUrl) { this.socialUrl = socialUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
