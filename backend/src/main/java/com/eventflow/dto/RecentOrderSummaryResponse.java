package com.eventflow.dto;

import com.eventflow.entity.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RecentOrderSummaryResponse {
    private String orderNumber;
    private String attendeeName;
    private String attendeeEmail;
    private int ticketCount;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;

    public RecentOrderSummaryResponse() {}

    public RecentOrderSummaryResponse(String orderNumber, String attendeeName, String attendeeEmail, int ticketCount, BigDecimal totalAmount, OrderStatus status, LocalDateTime createdAt) {
        this.orderNumber = orderNumber;
        this.attendeeName = attendeeName;
        this.attendeeEmail = attendeeEmail;
        this.ticketCount = ticketCount;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getAttendeeName() {
        return attendeeName;
    }

    public void setAttendeeName(String attendeeName) {
        this.attendeeName = attendeeName;
    }

    public String getAttendeeEmail() {
        return attendeeEmail;
    }

    public void setAttendeeEmail(String attendeeEmail) {
        this.attendeeEmail = attendeeEmail;
    }

    public int getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(int ticketCount) {
        this.ticketCount = ticketCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
