package com.eventflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TicketTierResponse {
    private Long id;
    private Long eventId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer capacity;
    private Integer soldCount;
    private Integer availableQuantity;
    private boolean isSoldOut;
    private boolean active;
    private LocalDateTime salesStartTime;
    private LocalDateTime salesEndTime;

    public TicketTierResponse() {}

    public TicketTierResponse(Long id, Long eventId, String name, String description, BigDecimal price,
                              Integer capacity, Integer soldCount, boolean active,
                              LocalDateTime salesStartTime, LocalDateTime salesEndTime) {
        this.id = id;
        this.eventId = eventId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.capacity = capacity;
        this.soldCount = soldCount;
        this.availableQuantity = Math.max(0, capacity - soldCount);
        this.isSoldOut = this.availableQuantity <= 0;
        this.active = active;
        this.salesStartTime = salesStartTime;
        this.salesEndTime = salesEndTime;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getSoldCount() {
        return soldCount;
    }

    public void setSoldCount(Integer soldCount) {
        this.soldCount = soldCount;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public boolean isSoldOut() {
        return isSoldOut;
    }

    public void setSoldOut(boolean soldOut) {
        isSoldOut = soldOut;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getSalesStartTime() {
        return salesStartTime;
    }

    public void setSalesStartTime(LocalDateTime salesStartTime) {
        this.salesStartTime = salesStartTime;
    }

    public LocalDateTime getSalesEndTime() {
        return salesEndTime;
    }

    public void setSalesEndTime(LocalDateTime salesEndTime) {
        this.salesEndTime = salesEndTime;
    }
}
