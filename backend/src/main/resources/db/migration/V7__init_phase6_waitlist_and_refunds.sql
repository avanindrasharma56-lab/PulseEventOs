-- V7__init_phase6_waitlist_and_refunds.sql
-- Phase 6: Waitlist Engine, Automated Reallocation, and Refund & Cancellation Workflows

CREATE TABLE IF NOT EXISTS `waitlist_entries` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `ticket_tier_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'WAITING',
    `queue_position` INT NOT NULL DEFAULT 1,
    `offer_expires_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_waitlist_tier` FOREIGN KEY (`ticket_tier_id`) REFERENCES `ticket_tiers` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_waitlist_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `uk_waitlist_tier_user` UNIQUE (`ticket_tier_id`, `user_id`),
    INDEX `idx_waitlist_tier_status` (`ticket_tier_id`, `status`),
    INDEX `idx_waitlist_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `refund_requests` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `amount` DECIMAL(10, 2) NOT NULL,
    `reason` VARCHAR(500) NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    `admin_notes` VARCHAR(500) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `processed_at` DATETIME NULL,
    CONSTRAINT `fk_refund_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_refund_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_refund_order` (`order_id`),
    INDEX `idx_refund_user` (`user_id`),
    INDEX `idx_refund_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
