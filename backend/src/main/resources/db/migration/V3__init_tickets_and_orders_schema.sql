-- ========================================================
-- Flyway Migration V3: Initialize Tickets, Tiers, Orders, and Check-In Schema
-- ========================================================

-- Ticket Tiers Table
CREATE TABLE IF NOT EXISTS ticket_tiers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    capacity INT NOT NULL,
    sold_count INT NOT NULL DEFAULT 0,
    sales_start_time DATETIME NULL,
    sales_end_time DATETIME NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tiers_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    INDEX idx_tiers_event (event_id),
    INDEX idx_tiers_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Orders Table
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    payment_reference VARCHAR(100) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_orders_number UNIQUE (order_number),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_orders_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    INDEX idx_orders_user (user_id),
    INDEX idx_orders_event (event_id),
    INDEX idx_orders_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Order Items Table
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    ticket_tier_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_tier FOREIGN KEY (ticket_tier_id) REFERENCES ticket_tiers (id) ON DELETE CASCADE,
    INDEX idx_items_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tickets Table (Individual passes with QR Code & Check-in status)
CREATE TABLE IF NOT EXISTS tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    ticket_tier_id BIGINT NOT NULL,
    ticket_code VARCHAR(64) NOT NULL,
    attendee_name VARCHAR(120) NOT NULL,
    attendee_email VARCHAR(180) NOT NULL,
    qr_code_data LONGTEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'VALID',
    checked_in_at DATETIME NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tickets_code UNIQUE (ticket_code),
    CONSTRAINT fk_tickets_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_tickets_tier FOREIGN KEY (ticket_tier_id) REFERENCES ticket_tiers (id) ON DELETE CASCADE,
    INDEX idx_tickets_order (order_id),
    INDEX idx_tickets_tier (ticket_tier_id),
    INDEX idx_tickets_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
