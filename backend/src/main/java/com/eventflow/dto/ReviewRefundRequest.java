package com.eventflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReviewRefundRequest {

    @NotNull(message = "Approval decision is required")
    private Boolean approved;

    @Size(max = 500, message = "Admin notes cannot exceed 500 characters")
    private String adminNotes;

    public ReviewRefundRequest() {}

    public ReviewRefundRequest(Boolean approved, String adminNotes) {
        this.approved = approved;
        this.adminNotes = adminNotes;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
}
