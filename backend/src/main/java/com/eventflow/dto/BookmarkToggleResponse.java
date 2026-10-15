package com.eventflow.dto;

public class BookmarkToggleResponse {
    private Long sessionId;
    private boolean bookmarked;
    private String message;

    public BookmarkToggleResponse() {}

    public BookmarkToggleResponse(Long sessionId, boolean bookmarked, String message) {
        this.sessionId = sessionId;
        this.bookmarked = bookmarked;
        this.message = message;
    }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public boolean isBookmarked() { return bookmarked; }
    public void setBookmarked(boolean bookmarked) { this.bookmarked = bookmarked; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
