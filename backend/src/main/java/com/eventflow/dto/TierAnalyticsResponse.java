package com.eventflow.dto;

import java.math.BigDecimal;

public class TierAnalyticsResponse {
    private Long tierId;
    private String name;
    private BigDecimal price;
    private int capacity;
    private int soldCount;
    private BigDecimal revenue;
    private long checkInCount;
    private double occupancyRate;

    public TierAnalyticsResponse() {}

    public TierAnalyticsResponse(Long tierId, String name, BigDecimal price, int capacity, int soldCount, BigDecimal revenue, long checkInCount, double occupancyRate) {
        this.tierId = tierId;
        this.name = name;
        this.price = price;
        this.capacity = capacity;
        this.soldCount = soldCount;
        this.revenue = revenue;
        this.checkInCount = checkInCount;
        this.occupancyRate = occupancyRate;
    }

    public Long getTierId() {
        return tierId;
    }

    public void setTierId(Long tierId) {
        this.tierId = tierId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getSoldCount() {
        return soldCount;
    }

    public void setSoldCount(int soldCount) {
        this.soldCount = soldCount;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public long getCheckInCount() {
        return checkInCount;
    }

    public void setCheckInCount(long checkInCount) {
        this.checkInCount = checkInCount;
    }

    public double getOccupancyRate() {
        return occupancyRate;
    }

    public void setOccupancyRate(double occupancyRate) {
        this.occupancyRate = occupancyRate;
    }
}
