-- V10__init_phase9_pricing_and_currencies.sql
-- Phase 9: Dynamic Surge / Early-Bird Pricing & Multi-Currency Engine (USD, EUR, GBP, INR)

-- 1. Currency Rates Table
CREATE TABLE IF NOT EXISTS `currency_rates` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `currency_code` VARCHAR(10) NOT NULL UNIQUE,
    `currency_name` VARCHAR(50) NOT NULL,
    `symbol` VARCHAR(10) NOT NULL,
    `rate_to_usd` DECIMAL(12, 6) NOT NULL DEFAULT 1.000000,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed default currencies
INSERT INTO `currency_rates` (`currency_code`, `currency_name`, `symbol`, `rate_to_usd`) VALUES
('USD', 'US Dollar', '$', 1.000000),
('EUR', 'Euro', '€', 0.925000),
('GBP', 'British Pound', '£', 0.790000),
('INR', 'Indian Rupee', '₹', 84.250000)
ON DUPLICATE KEY UPDATE `rate_to_usd` = VALUES(`rate_to_usd`);

-- 2. Dynamic Pricing Rules Table
CREATE TABLE IF NOT EXISTS `dynamic_pricing_rules` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `event_id` BIGINT NOT NULL,
    `rule_type` VARCHAR(50) NOT NULL, -- CAPACITY_SURGE, EARLY_BIRD, LAST_MINUTE
    `threshold_percent` INT NULL,     -- e.g. 80 for 80% capacity
    `multiplier` DECIMAL(5, 2) NOT NULL DEFAULT 1.00, -- e.g. 1.15 (+15%), 0.85 (-15%)
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_pricing_rule_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`) ON DELETE CASCADE,
    INDEX `idx_pricing_rule_event` (`event_id`, `is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
