package com.eventflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CreateOrderRequest {

    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotEmpty(message = "Order must contain at least one ticket tier selection")
    @Valid
    private List<CreateOrderItemRequest> items;

    private String promoCode;

    public CreateOrderRequest() {}

    public CreateOrderRequest(Long eventId, List<CreateOrderItemRequest> items) {
        this.eventId = eventId;
        this.items = items;
    }

    public CreateOrderRequest(Long eventId, List<CreateOrderItemRequest> items, String promoCode) {
        this.eventId = eventId;
        this.items = items;
        this.promoCode = promoCode;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public List<CreateOrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<CreateOrderItemRequest> items) {
        this.items = items;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }
}
