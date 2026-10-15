package com.eventflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CreateOrderItemRequest {

    @NotNull(message = "Ticket tier ID is required")
    private Long ticketTierId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 10, message = "Maximum 10 tickets per purchase")
    private Integer quantity;

    private String attendeeName;
    private String attendeeEmail;

    public CreateOrderItemRequest() {}

    public CreateOrderItemRequest(Long ticketTierId, Integer quantity) {
        this.ticketTierId = ticketTierId;
        this.quantity = quantity;
    }

    public CreateOrderItemRequest(Long ticketTierId, Integer quantity, String attendeeName, String attendeeEmail) {
        this.ticketTierId = ticketTierId;
        this.quantity = quantity;
        this.attendeeName = attendeeName;
        this.attendeeEmail = attendeeEmail;
    }

    public Long getTicketTierId() {
        return ticketTierId;
    }

    public void setTicketTierId(Long ticketTierId) {
        this.ticketTierId = ticketTierId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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
}
