-- V14__init_phase16_breakout_session_rsvps.sql
-- Phase 16: Multi-Track Breakout Session Room Booking, RSVP Engine & Time Conflict Resolver

-- 1. Add capacity and reservation tracking to event_sessions
ALTER TABLE `event_sessions`
    ADD COLUMN `max_capacity` INT NOT NULL DEFAULT 100,
    ADD COLUMN `reserved_seats` INT NOT NULL DEFAULT 0;

-- 2. Create session_rsvps table
CREATE TABLE IF NOT EXISTS `session_rsvps` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `session_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    `rsvp_code` VARCHAR(40) NOT NULL,
    `reserved_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_rsvp_session` FOREIGN KEY (`session_id`) REFERENCES `event_sessions` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_rsvp_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `uk_session_user_rsvp` UNIQUE (`session_id`, `user_id`),
    CONSTRAINT `uk_rsvp_code` UNIQUE (`rsvp_code`),
    INDEX `idx_rsvp_session_status` (`session_id`, `status`),
    INDEX `idx_rsvp_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
