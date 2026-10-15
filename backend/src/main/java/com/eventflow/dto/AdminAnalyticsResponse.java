package com.eventflow.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AdminAnalyticsResponse {
    private long totalUsers;
    private long totalOrganizers;
    private long totalAttendees;
    private int totalEvents;
    private int publishedEvents;
    private int pendingApprovalEvents;
    private int totalTicketsSold;
    private BigDecimal totalPlatformRevenue;
    private long totalCheckIns;
    private List<EventAnalyticsResponse> topEvents = new ArrayList<>();

    public AdminAnalyticsResponse() {}

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalOrganizers() {
        return totalOrganizers;
    }

    public void setTotalOrganizers(long totalOrganizers) {
        this.totalOrganizers = totalOrganizers;
    }

    public long getTotalAttendees() {
        return totalAttendees;
    }

    public void setTotalAttendees(long totalAttendees) {
        this.totalAttendees = totalAttendees;
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(int totalEvents) {
        this.totalEvents = totalEvents;
    }

    public int getPublishedEvents() {
        return publishedEvents;
    }

    public void setPublishedEvents(int publishedEvents) {
        this.publishedEvents = publishedEvents;
    }

    public int getPendingApprovalEvents() {
        return pendingApprovalEvents;
    }

    public void setPendingApprovalEvents(int pendingApprovalEvents) {
        this.pendingApprovalEvents = pendingApprovalEvents;
    }

    public int getTotalTicketsSold() {
        return totalTicketsSold;
    }

    public void setTotalTicketsSold(int totalTicketsSold) {
        this.totalTicketsSold = totalTicketsSold;
    }

    public BigDecimal getTotalPlatformRevenue() {
        return totalPlatformRevenue;
    }

    public void setTotalPlatformRevenue(BigDecimal totalPlatformRevenue) {
        this.totalPlatformRevenue = totalPlatformRevenue;
    }

    public long getTotalCheckIns() {
        return totalCheckIns;
    }

    public void setTotalCheckIns(long totalCheckIns) {
        this.totalCheckIns = totalCheckIns;
    }

    public List<EventAnalyticsResponse> getTopEvents() {
        return topEvents;
    }

    public void setTopEvents(List<EventAnalyticsResponse> topEvents) {
        this.topEvents = topEvents;
    }
}
