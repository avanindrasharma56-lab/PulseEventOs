-- ========================================================
-- Flyway Migration V2: Initialize Events, Categories, & Venues
-- ========================================================

-- Event Categories Table
CREATE TABLE IF NOT EXISTS event_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    description TEXT NULL,
    icon_name VARCHAR(50) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_categories_name UNIQUE (name),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Venues Table
CREATE TABLE IF NOT EXISTS venues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(255) NULL,
    city VARCHAR(100) NULL,
    state VARCHAR(100) NULL,
    postal_code VARCHAR(20) NULL,
    country VARCHAR(100) NULL,
    capacity INT NULL,
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    stream_url VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_venues_city (city),
    INDEX idx_venues_online (is_online)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Events Table
CREATE TABLE IF NOT EXISTS events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organizer_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    venue_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL,
    short_summary TEXT NULL,
    description LONGTEXT NULL,
    banner_image_url VARCHAR(500) NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    rejection_reason TEXT NULL,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_events_slug UNIQUE (slug),
    CONSTRAINT fk_events_organizer FOREIGN KEY (organizer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_events_category FOREIGN KEY (category_id) REFERENCES event_categories (id),
    CONSTRAINT fk_events_venue FOREIGN KEY (venue_id) REFERENCES venues (id) ON DELETE SET NULL,
    INDEX idx_events_status_start (status, start_time),
    INDEX idx_events_organizer (organizer_id),
    INDEX idx_events_category (category_id),
    INDEX idx_events_featured (is_featured)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed Default Categories
INSERT INTO event_categories (name, slug, description, icon_name, is_active) VALUES
('Technology & AI', 'tech-ai', 'Developer conferences, AI summits, and technology showcases', 'Cpu', TRUE),
('Music & Festivals', 'music-festivals', 'Live concerts, acoustic performances, and music festivals', 'Music', TRUE),
('Business & Networking', 'business-networking', 'Founder mixers, startup pitching, and enterprise leadership forums', 'Briefcase', TRUE),
('Workshops & Masterclasses', 'workshops-masterclasses', 'Hands-on bootcamps, masterclasses, and certified workshops', 'GraduationCap', TRUE),
('Gaming & Esports', 'gaming-esports', 'LAN tournaments, game developer showcases, and live esports', 'Gamepad2', TRUE),
('Arts & Culture', 'arts-culture', 'Art exhibitions, theatrical performances, and film festivals', 'Palette', TRUE)
ON DUPLICATE KEY UPDATE name = name;

-- Seed Default Venues
INSERT INTO venues (name, address, city, state, postal_code, country, capacity, is_online, stream_url) VALUES
('Moscone West Convention Center', '747 Howard St', 'San Francisco', 'CA', '94103', 'USA', 5000, FALSE, NULL),
('Austin City Music Hall', '310 Willie Nelson Blvd', 'Austin', 'TX', '78701', 'USA', 2500, FALSE, NULL),
('Global Pulse Virtual Auditorium', NULL, 'Online', 'Virtual', NULL, 'Worldwide', 50000, TRUE, 'https://stream.pulsevent.io/live')
ON DUPLICATE KEY UPDATE name = name;
