package com.eventflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateSpeakerRequest {

    private Long eventId;

    @NotBlank(message = "Speaker name is required")
    private String name;

    private String title;
    private String company;
    private String bio;
    private String avatarUrl;
    private String socialUrl;

    public CreateSpeakerRequest() {}

    public CreateSpeakerRequest(Long eventId, String name, String title, String company, String bio, String avatarUrl, String socialUrl) {
        this.eventId = eventId;
        this.name = name;
        this.title = title;
        this.company = company;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
        this.socialUrl = socialUrl;
    }

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
}
