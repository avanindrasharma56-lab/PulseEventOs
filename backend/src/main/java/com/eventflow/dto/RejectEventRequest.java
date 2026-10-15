package com.eventflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RejectEventRequest {

    @NotBlank(message = "A rejection reason is mandatory")
    @Size(min = 5, max = 1000, message = "Rejection reason must be between 5 and 1000 characters")
    private String reason;

    public RejectEventRequest() {}

    public RejectEventRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
