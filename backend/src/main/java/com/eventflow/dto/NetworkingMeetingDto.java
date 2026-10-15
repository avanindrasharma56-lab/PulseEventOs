package com.eventflow.dto;

import com.eventflow.entity.MeetingStatus;
import java.time.LocalDateTime;

public class NetworkingMeetingDto {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private Long requesterId;
    private String requesterName;
    private String requesterCompany;
    private String requesterHeadline;
    private Long recipientId;
    private String recipientName;
    private String recipientCompany;
    private String recipientHeadline;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer tableNumber;
    private String locationName;
    private String message;
    private MeetingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public NetworkingMeetingDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRequesterCompany() {
        return requesterCompany;
    }

    public void setRequesterCompany(String requesterCompany) {
        this.requesterCompany = requesterCompany;
    }

    public String getRequesterHeadline() {
        return requesterHeadline;
    }

    public void setRequesterHeadline(String requesterHeadline) {
        this.requesterHeadline = requesterHeadline;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientCompany() {
        return recipientCompany;
    }

    public void setRecipientCompany(String recipientCompany) {
        this.recipientCompany = recipientCompany;
    }

    public String getRecipientHeadline() {
        return recipientHeadline;
    }

    public void setRecipientHeadline(String recipientHeadline) {
        this.recipientHeadline = recipientHeadline;
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

    public Integer getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(Integer tableNumber) {
        this.tableNumber = tableNumber;
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

    public MeetingStatus getStatus() {
        return status;
    }

    public void setStatus(MeetingStatus status) {
        this.status = status;
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
