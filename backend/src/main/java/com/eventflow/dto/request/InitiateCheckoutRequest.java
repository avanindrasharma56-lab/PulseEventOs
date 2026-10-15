package com.eventflow.dto.request;

import com.eventflow.entity.enums.PaymentProvider;
import jakarta.validation.constraints.NotNull;

public class InitiateCheckoutRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    private PaymentProvider provider = PaymentProvider.STRIPE;

    private String successUrl;

    private String cancelUrl;

    public InitiateCheckoutRequest() {}

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public PaymentProvider getProvider() {
        return provider;
    }

    public void setProvider(PaymentProvider provider) {
        this.provider = provider;
    }

    public String getSuccessUrl() {
        return successUrl;
    }

    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }
}
