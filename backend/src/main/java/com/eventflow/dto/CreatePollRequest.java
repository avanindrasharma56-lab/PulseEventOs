package com.eventflow.dto;

import java.util.List;

public class CreatePollRequest {

    private String question;
    private Long sessionId;
    private List<String> options;

    public CreatePollRequest() {
    }

    public CreatePollRequest(String question, Long sessionId, List<String> options) {
        this.question = question;
        this.sessionId = sessionId;
        this.options = options;
    }

    public CreatePollRequest(String question, List<String> options) {
        this(question, null, options);
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}
