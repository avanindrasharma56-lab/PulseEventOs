package com.eventflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateRefundRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    public CreateRefundRequest() {}

    public CreateRefundRequest(Long orderId, String reason) {
        this.orderId = orderId;
        this.reason = reason;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
