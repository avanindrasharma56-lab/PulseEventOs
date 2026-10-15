package com.eventflow.dto;

import com.eventflow.entity.enums.QuestionStatus;

public class QuestionStatusUpdateRequest {

    private QuestionStatus status;

    public QuestionStatusUpdateRequest() {
    }

    public QuestionStatusUpdateRequest(QuestionStatus status) {
        this.status = status;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionStatus status) {
        this.status = status;
    }
}
