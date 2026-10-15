package com.eventflow.dto;

public class PollVoteRequest {

    private Long optionId;

    public PollVoteRequest() {
    }

    public PollVoteRequest(Long optionId) {
        this.optionId = optionId;
    }

    public Long getOptionId() {
        return optionId;
    }

    public void setOptionId(Long optionId) {
        this.optionId = optionId;
    }
}
