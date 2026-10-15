package com.eventflow.entity;

public enum GamificationTier {
    NOVICE(0),
    BRONZE(100),
    SILVER(250),
    GOLD(500),
    PLATINUM(900);

    private final int minXp;

    GamificationTier(int minXp) {
        this.minXp = minXp;
    }

    public int getMinXp() {
        return minXp;
    }

    public static GamificationTier fromXp(int xp) {
        if (xp >= PLATINUM.minXp) return PLATINUM;
        if (xp >= GOLD.minXp) return GOLD;
        if (xp >= SILVER.minXp) return SILVER;
        if (xp >= BRONZE.minXp) return BRONZE;
        return NOVICE;
    }
}
