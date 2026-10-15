package com.eventflow.dto;

import com.eventflow.entity.enums.WaitlistStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WaitlistEntryResponse {

    private Long id;
    private Long ticketTierId;
    private String tierName;
    private BigDecimal tierPrice;
    private Long eventId;
    private String eventTitle;
    private Long userId;
    private String userName;
    private String userEmail;
    private WaitlistStatus status;
    private int queuePosition;
    private LocalDateTime offerExpiresAt;
    private LocalDateTime createdAt;

    public WaitlistEntryResponse() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTicketTierId() {
        return ticketTierId;
    }

    public void setTicketTierId(Long ticketTierId) {
        this.ticketTierId = ticketTierId;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public BigDecimal getTierPrice() {
        return tierPrice;
    }

    public void setTierPrice(BigDecimal tierPrice) {
        this.tierPrice = tierPrice;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
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
}
