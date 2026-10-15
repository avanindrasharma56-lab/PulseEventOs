-- =========================================================================
-- Flyway Migration V11: Phase 12 - Sponsors, Exhibitor Floor & Lead Retrieval
-- =========================================================================

CREATE TABLE IF NOT EXISTS sponsors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    tier VARCHAR(50) NOT NULL, -- TITLE, PLATINUM, GOLD, SILVER, BRONZE
    logo_url VARCHAR(512),
    website_url VARCHAR(512),
    booth_number VARCHAR(50),
    description TEXT,
    display_order INT DEFAULT 0,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_sponsors_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS lead_retrievals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sponsor_id BIGINT NOT NULL,
    attendee_id BIGINT NOT NULL,
    ticket_code VARCHAR(64) NOT NULL,
    attendee_name VARCHAR(255) NOT NULL,
    attendee_email VARCHAR(255) NOT NULL,
    notes TEXT,
    rating INT DEFAULT 3, -- 1 to 5 lead qualification
    scanned_at DATETIME NOT NULL,
    CONSTRAINT fk_leads_sponsor FOREIGN KEY (sponsor_id) REFERENCES sponsors(id) ON DELETE CASCADE,
    CONSTRAINT fk_leads_attendee FOREIGN KEY (attendee_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_sponsors_event_tier ON sponsors(event_id, tier);
CREATE INDEX idx_leads_sponsor_scanned ON lead_retrievals(sponsor_id, scanned_at);

-- Seed premier conference sponsors for Pulse Developer Summit
INSERT INTO sponsors (event_id, name, tier, logo_url, website_url, booth_number, description, display_order, created_at)
SELECT id, 'CloudScale Technologies', 'TITLE', 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe', 'https://cloudscale.io', 'Booth #T-01 (Grand Hallway)', 'Global hyperscale serverless cloud platform with 99.999% SLA.', 1, NOW()
FROM events WHERE slug LIKE 'pulse-developer-summit-2026%' LIMIT 1;

INSERT INTO sponsors (event_id, name, tier, logo_url, website_url, booth_number, description, display_order, created_at)
SELECT id, 'CognitiveScale AI Labs', 'PLATINUM', 'https://images.unsplash.com/photo-1620712943543-bcc4688e7485', 'https://cognitivescale.ai', 'Booth #P-12 (Expo A)', 'Pioneering multimodal foundational models for autonomous infrastructure.', 2, NOW()
FROM events WHERE slug LIKE 'pulse-developer-summit-2026%' LIMIT 1;

INSERT INTO sponsors (event_id, name, tier, logo_url, website_url, booth_number, description, display_order, created_at)
SELECT id, 'SecurPulse Zero-Trust', 'GOLD', 'https://images.unsplash.com/photo-1563986768609-322da13575f3', 'https://securpulse.io', 'Booth #G-08 (Expo B)', 'Hardware-grade cryptographic security and identity verification for live gates.', 3, NOW()
FROM events WHERE slug LIKE 'pulse-developer-summit-2026%' LIMIT 1;
