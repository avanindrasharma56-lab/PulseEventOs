-- V12__init_phase14_certificates.sql
-- Certificate of Attendance & Completion Engine

CREATE TABLE IF NOT EXISTS event_certificates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    certificate_code VARCHAR(64) NOT NULL UNIQUE,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    ticket_id BIGINT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    event_title VARCHAR(255) NOT NULL,
    issue_date DATE NOT NULL,
    certificate_type VARCHAR(64) NOT NULL DEFAULT 'COMPLETION',
    hours_completed INT NOT NULL DEFAULT 16,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cert_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    CONSTRAINT fk_cert_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_cert_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE SET NULL,
    CONSTRAINT uk_cert_event_user UNIQUE (event_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_cert_code ON event_certificates (certificate_code);
CREATE INDEX idx_cert_user ON event_certificates (user_id);
CREATE INDEX idx_cert_event ON event_certificates (event_id);
