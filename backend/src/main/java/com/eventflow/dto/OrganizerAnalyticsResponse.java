package com.eventflow.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OrganizerAnalyticsResponse {
    private int totalEvents;
    private int publishedEvents;
    private int draftEvents;
    private int pendingApprovalEvents;
    private int totalTicketsSold;
    private int totalCapacity;
    private BigDecimal totalRevenue;
    private long totalCheckIns;
    private double overallCheckInRate;
    private List<EventAnalyticsResponse> events = new ArrayList<>();

    public OrganizerAnalyticsResponse() {}

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

    public int getDraftEvents() {
        return draftEvents;
    }

    public void setDraftEvents(int draftEvents) {
        this.draftEvents = draftEvents;
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

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(int totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public long getTotalCheckIns() {
        return totalCheckIns;
    }

    public void setTotalCheckIns(long totalCheckIns) {
        this.totalCheckIns = totalCheckIns;
    }

    public double getOverallCheckInRate() {
        return overallCheckInRate;
    }

    public void setOverallCheckInRate(double overallCheckInRate) {
        this.overallCheckInRate = overallCheckInRate;
    }

    public List<EventAnalyticsResponse> getEvents() {
        return events;
    }

    public void setEvents(List<EventAnalyticsResponse> events) {
        this.events = events;
    }
}
