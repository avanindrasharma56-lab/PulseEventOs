package com.eventflow.dto;

import com.eventflow.entity.enums.PollStatus;

public class PollStatusUpdateRequest {

    private PollStatus status;

    public PollStatusUpdateRequest() {
    }

    public PollStatusUpdateRequest(PollStatus status) {
        this.status = status;
    }

    public PollStatus getStatus() {
        return status;
    }

    public void setStatus(PollStatus status) {
        this.status = status;
    }
}
