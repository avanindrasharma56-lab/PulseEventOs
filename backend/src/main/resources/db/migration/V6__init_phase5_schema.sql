-- ========================================================
-- Flyway Migration V6: Initialize Phase 5 Schema
-- 1. Promo / Discount Codes
-- 2. Event Reviews & Star Ratings
-- 3. Order Discount & Promo Code Association
-- ========================================================

-- 1. Promo Codes Table
CREATE TABLE IF NOT EXISTS promo_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    discount_type VARCHAR(30) NOT NULL DEFAULT 'PERCENTAGE',
    discount_value DECIMAL(10,2) NOT NULL,
    max_uses INT NULL,
    current_uses INT NOT NULL DEFAULT 0,
    min_order_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    valid_from DATETIME NULL,
    valid_until DATETIME NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    event_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_promo_code UNIQUE (code),
    CONSTRAINT fk_promo_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE SET NULL,
    INDEX idx_promo_code (code),
    INDEX idx_promo_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Event Reviews Table
CREATE TABLE IF NOT EXISTS event_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    title VARCHAR(120) NULL,
    comment TEXT NULL,
    is_verified_attendee BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_reviews_user_event UNIQUE (user_id, event_id),
    CONSTRAINT fk_reviews_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_reviews_event (event_id),
    INDEX idx_reviews_rating (rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Update Orders Table with Discount tracking
ALTER TABLE orders 
ADD COLUMN discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 AFTER total_amount,
ADD COLUMN promo_code_id BIGINT NULL AFTER discount_amount;

ALTER TABLE orders
ADD CONSTRAINT fk_orders_promo FOREIGN KEY (promo_code_id) REFERENCES promo_codes (id) ON DELETE SET NULL;

-- 4. Seed Default Promo Codes
INSERT INTO promo_codes (code, description, discount_type, discount_value, max_uses, min_order_amount, is_active)
VALUES 
('PULSE20', '20% off all event tickets & passes', 'PERCENTAGE', 20.00, 500, 0.00, TRUE),
('SAVE50', '$50 flat discount on orders over $100', 'FIXED_AMOUNT', 50.00, 100, 100.00, TRUE),
('VIPFREE', '100% complimentary VIP invitation pass', 'PERCENTAGE', 100.00, 50, 0.00, TRUE);
