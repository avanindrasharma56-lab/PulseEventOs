-- ========================================================
-- Flyway Migration V5: Initialize Payments & Webhook Transactions Schema
-- ========================================================

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_reference VARCHAR(100) NOT NULL,
    order_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL DEFAULT 'STRIPE',
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    session_id VARCHAR(255) NULL,
    client_secret VARCHAR(255) NULL,
    transaction_id VARCHAR(255) NULL,
    payment_method VARCHAR(50) NULL,
    failure_reason VARCHAR(255) NULL,
    raw_response LONGTEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_payments_ref UNIQUE (payment_reference),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    INDEX idx_payments_order (order_id),
    INDEX idx_payments_session (session_id),
    INDEX idx_payments_status (status),
    INDEX idx_payments_provider (provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
