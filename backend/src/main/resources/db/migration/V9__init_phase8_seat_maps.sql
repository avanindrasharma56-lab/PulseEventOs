-- V9__init_phase8_seat_maps.sql
-- Phase 8: Interactive Visual Venue Seat Map, 5-Minute Hold Locks & Reserved Seating Engine

-- 1. Venue Seat Sections Table
CREATE TABLE IF NOT EXISTS `venue_seat_sections` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `venue_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `tier_id` BIGINT NULL,
    `row_count` INT NOT NULL DEFAULT 5,
    `seats_per_row` INT NOT NULL DEFAULT 10,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_seat_section_venue` FOREIGN KEY (`venue_id`) REFERENCES `venues` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_seat_section_tier` FOREIGN KEY (`tier_id`) REFERENCES `ticket_tiers` (`id`) ON DELETE SET NULL,
    INDEX `idx_section_venue` (`venue_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Venue Individual Seats Table
CREATE TABLE IF NOT EXISTS `venue_seats` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `section_id` BIGINT NOT NULL,
    `row_label` VARCHAR(10) NOT NULL,
    `seat_number` INT NOT NULL,
    `seat_code` VARCHAR(30) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_seat_section` FOREIGN KEY (`section_id`) REFERENCES `venue_seat_sections` (`id`) ON DELETE CASCADE,
    CONSTRAINT `uk_section_row_seat` UNIQUE (`section_id`, `row_label`, `seat_number`),
    INDEX `idx_seat_section` (`section_id`),
    INDEX `idx_seat_code` (`seat_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Event Seat Reservations Table (with 5-minute Hold Locks)
CREATE TABLE IF NOT EXISTS `event_seat_reservations` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `event_id` BIGINT NOT NULL,
    `seat_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `order_id` BIGINT NULL,
    `ticket_id` BIGINT NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'HOLD',
    `expires_at` DATETIME NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_reservation_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_reservation_seat` FOREIGN KEY (`seat_id`) REFERENCES `venue_seats` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_reservation_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_reservation_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_reservation_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`) ON DELETE SET NULL,
    CONSTRAINT `uk_event_seat` UNIQUE (`event_id`, `seat_id`),
    INDEX `idx_reservation_event_status` (`event_id`, `status`),
    INDEX `idx_reservation_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
