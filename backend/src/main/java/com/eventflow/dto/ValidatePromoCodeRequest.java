package com.eventflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ValidatePromoCodeRequest {

    @NotBlank(message = "Promo code is required")
    private String code;

    private Long eventId;

    @NotNull(message = "Order total is required")
    private BigDecimal orderTotal;

    public ValidatePromoCodeRequest() {}

    public ValidatePromoCodeRequest(String code, Long eventId, BigDecimal orderTotal) {
        this.code = code;
        this.eventId = eventId;
        this.orderTotal = orderTotal;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public BigDecimal getOrderTotal() {
        return orderTotal;
    }

    public void setOrderTotal(BigDecimal orderTotal) {
        this.orderTotal = orderTotal;
    }
}
