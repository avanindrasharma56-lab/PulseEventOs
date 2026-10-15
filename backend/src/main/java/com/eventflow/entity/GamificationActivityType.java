package com.eventflow.entity;

public enum GamificationActivityType {
    TURNSTILE_CHECKIN(100),
    SPONSOR_BOOTH_SCAN(50),
    SESSION_ATTENDANCE(75),
    QA_QUESTION(25),
    POLL_VOTE(25),
    NETWORKING_MEETING(80),
    FEEDBACK_REVIEW(50);

    private final int defaultXp;

    GamificationActivityType(int defaultXp) {
        this.defaultXp = defaultXp;
    }

    public int getDefaultXp() {
        return defaultXp;
    }
}
