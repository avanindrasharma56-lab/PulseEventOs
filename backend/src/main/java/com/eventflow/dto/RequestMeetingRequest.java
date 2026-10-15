package com.eventflow.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class RequestMeetingRequest {
    @NotNull(message = "Recipient user ID is required")
    private Long recipientId;

    @NotNull(message = "Meeting start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "Meeting end time is required")
    private LocalDateTime endTime;

    private String locationName;
    private String message;

    public RequestMeetingRequest() {
    }

    public RequestMeetingRequest(Long recipientId, LocalDateTime startTime, LocalDateTime endTime,
                                String locationName, String message) {
        this.recipientId = recipientId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.locationName = locationName;
        this.message = message;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
