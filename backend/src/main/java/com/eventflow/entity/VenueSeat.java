package com.eventflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "venue_seats", uniqueConstraints = {
    @UniqueConstraint(name = "uk_section_row_seat", columnNames = {"section_id", "row_label", "seat_number"})
})
public class VenueSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private VenueSeatSection section;

    @Column(name = "row_label", nullable = false, length = 10)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Column(name = "seat_code", nullable = false, length = 30)
    private String seatCode;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public VenueSeat() {}

    public VenueSeat(VenueSeatSection section, String rowLabel, Integer seatNumber, String seatCode) {
        this.section = section;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.seatCode = seatCode;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public VenueSeatSection getSection() { return section; }
    public void setSection(VenueSeatSection section) { this.section = section; }

    public String getRowLabel() { return rowLabel; }
    public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }

    public Integer getSeatNumber() { return seatNumber; }
    public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }

    public String getSeatCode() { return seatCode; }
    public void setSeatCode(String seatCode) { this.seatCode = seatCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
