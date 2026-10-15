package com.eventflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "venue_seat_sections")
public class VenueSeatSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id")
    private TicketTier tier;

    @Column(name = "row_count", nullable = false)
    private Integer rowCount = 5;

    @Column(name = "seats_per_row", nullable = false)
    private Integer seatsPerRow = 10;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public VenueSeatSection() {}

    public VenueSeatSection(Venue venue, String name, TicketTier tier, Integer rowCount, Integer seatsPerRow) {
        this.venue = venue;
        this.name = name;
        this.tier = tier;
        this.rowCount = rowCount != null ? rowCount : 5;
        this.seatsPerRow = seatsPerRow != null ? seatsPerRow : 10;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Venue getVenue() { return venue; }
    public void setVenue(Venue venue) { this.venue = venue; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public TicketTier getTier() { return tier; }
    public void setTier(TicketTier tier) { this.tier = tier; }

    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }

    public Integer getSeatsPerRow() { return seatsPerRow; }
    public void setSeatsPerRow(Integer seatsPerRow) { this.seatsPerRow = seatsPerRow; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
