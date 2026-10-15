package com.eventflow.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class AiCopyRequest {

    @NotBlank(message = "Topic or event idea is required")
    private String topic;

    private String category;
    private String targetAudience;
    private String tone = "Professional"; // Professional, Visionary, Exciting, Technical
    private String keyHighlights;

    public AiCopyRequest() {}

    public AiCopyRequest(String topic, String category, String targetAudience, String tone, String keyHighlights) {
        this.topic = topic;
        this.category = category;
        this.targetAudience = targetAudience;
        this.tone = tone != null ? tone : "Professional";
        this.keyHighlights = keyHighlights;
    }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }

    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }

    public String getKeyHighlights() { return keyHighlights; }
    public void setKeyHighlights(String keyHighlights) { this.keyHighlights = keyHighlights; }
}
