-- V8__init_phase7_agenda_and_speakers.sql
-- Phase 7: Multi-Day Conference Agenda, Speaker Profiles, Attendee Bookmarks & iCal Timetable Engine

-- 1. Speakers Table
CREATE TABLE IF NOT EXISTS `speakers` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `event_id` BIGINT NOT NULL,
    `name` VARCHAR(150) NOT NULL,
    `title` VARCHAR(150) NULL,
    `company` VARCHAR(150) NULL,
    `bio` TEXT NULL,
    `avatar_url` VARCHAR(500) NULL,
    `social_url` VARCHAR(255) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_speaker_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`) ON DELETE CASCADE,
    INDEX `idx_speaker_event` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Event Sessions / Agenda Table
CREATE TABLE IF NOT EXISTS `event_sessions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `event_id` BIGINT NOT NULL,
    `speaker_id` BIGINT NULL,
    `title` VARCHAR(255) NOT NULL,
    `description` TEXT NULL,
    `room` VARCHAR(100) NULL,
    `track` VARCHAR(100) NULL,
    `session_type` VARCHAR(50) NOT NULL DEFAULT 'KEYNOTE',
    `start_time` DATETIME NOT NULL,
    `end_time` DATETIME NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_session_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_session_speaker` FOREIGN KEY (`speaker_id`) REFERENCES `speakers` (`id`) ON DELETE SET NULL,
    INDEX `idx_session_event_start` (`event_id`, `start_time`),
    INDEX `idx_session_track` (`track`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Attendee Personal Schedule Bookmarks Table
CREATE TABLE IF NOT EXISTS `user_schedule_bookmarks` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `session_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_bookmark_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_bookmark_session` FOREIGN KEY (`session_id`) REFERENCES `event_sessions` (`id`) ON DELETE CASCADE,
    CONSTRAINT `uk_user_session_bookmark` UNIQUE (`user_id`, `session_id`),
    INDEX `idx_bookmark_user` (`user_id`),
    INDEX `idx_bookmark_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
