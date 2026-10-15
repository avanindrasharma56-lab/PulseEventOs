package com.eventflow.dto;

public class QuestionSubmitRequest {

    private String questionText;
    private boolean anonymous = false;
    private Long sessionId;

    public QuestionSubmitRequest() {
    }

    public QuestionSubmitRequest(String questionText, boolean anonymous, Long sessionId) {
        this.questionText = questionText;
        this.anonymous = anonymous;
        this.sessionId = sessionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
}
