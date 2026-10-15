package com.eventflow.dto;

import java.math.BigDecimal;
import java.util.List;

public class SeatSectionDto {
    private Long id;
    private String name;
    private Long tierId;
    private String tierName;
    private BigDecimal price;
    private Integer rowCount;
    private Integer seatsPerRow;
    private List<SeatDto> seats;

    public SeatSectionDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getTierId() { return tierId; }
    public void setTierId(Long tierId) { this.tierId = tierId; }

    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }

    public Integer getSeatsPerRow() { return seatsPerRow; }
    public void setSeatsPerRow(Integer seatsPerRow) { this.seatsPerRow = seatsPerRow; }

    public List<SeatDto> getSeats() { return seats; }
    public void setSeats(List<SeatDto> seats) { this.seats = seats; }
}
