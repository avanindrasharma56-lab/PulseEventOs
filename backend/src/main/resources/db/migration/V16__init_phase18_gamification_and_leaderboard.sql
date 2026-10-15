-- ====================================================================
-- Phase 18: Gamified Attendee Engagement, Scavenger Hunt & Leaderboard
-- ====================================================================

CREATE TABLE IF NOT EXISTS attendee_gamification_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    total_xp INT NOT NULL DEFAULT 0,
    current_tier VARCHAR(30) NOT NULL DEFAULT 'NOVICE',
    rank_position INT DEFAULT NULL,
    swag_code VARCHAR(50) DEFAULT NULL,
    is_swag_claimed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_gamify_profile_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    CONSTRAINT fk_gamify_profile_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_gamify_user_event UNIQUE (event_id, user_id),
    INDEX idx_gamify_xp (event_id, total_xp DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS gamification_activity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    reference_id VARCHAR(100) DEFAULT NULL,
    xp_earned INT NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_gamify_log_profile FOREIGN KEY (profile_id) REFERENCES attendee_gamification_profiles (id) ON DELETE CASCADE,
    CONSTRAINT uk_gamify_log_activity UNIQUE (profile_id, activity_type, reference_id),
    INDEX idx_gamify_log_profile (profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS attendee_badges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    badge_key VARCHAR(50) NOT NULL,
    badge_name VARCHAR(100) NOT NULL,
    badge_icon VARCHAR(50) NOT NULL,
    awarded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_gamify_badge_profile FOREIGN KEY (profile_id) REFERENCES attendee_gamification_profiles (id) ON DELETE CASCADE,
    CONSTRAINT uk_badge_profile_key UNIQUE (profile_id, badge_key),
    INDEX idx_badge_profile (profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
