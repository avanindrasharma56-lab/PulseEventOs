package com.eventflow.dto;

import com.eventflow.entity.enums.EventStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventAnalyticsResponse {
    private Long eventId;
    private String title;
    private String slug;
    private EventStatus status;
    private LocalDateTime startTime;
    private BigDecimal totalRevenue;
    private int ticketsSold;
    private int totalCapacity;
    private long checkInCount;
    private double checkInRate;
    private List<TierAnalyticsResponse> tierBreakdowns = new ArrayList<>();
    private List<RecentOrderSummaryResponse> recentOrders = new ArrayList<>();

    public EventAnalyticsResponse() {}

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public int getTicketsSold() {
        return ticketsSold;
    }

    public void setTicketsSold(int ticketsSold) {
        this.ticketsSold = ticketsSold;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(int totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public long getCheckInCount() {
        return checkInCount;
    }

    public void setCheckInCount(long checkInCount) {
        this.checkInCount = checkInCount;
    }

    public double getCheckInRate() {
        return checkInRate;
    }

    public void setCheckInRate(double checkInRate) {
        this.checkInRate = checkInRate;
    }

    public List<TierAnalyticsResponse> getTierBreakdowns() {
        return tierBreakdowns;
    }

    public void setTierBreakdowns(List<TierAnalyticsResponse> tierBreakdowns) {
        this.tierBreakdowns = tierBreakdowns;
    }

    public List<RecentOrderSummaryResponse> getRecentOrders() {
        return recentOrders;
    }

    public void setRecentOrders(List<RecentOrderSummaryResponse> recentOrders) {
        this.recentOrders = recentOrders;
    }
}
