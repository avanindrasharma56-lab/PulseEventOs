package com.eventflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateTicketTierRequest {

    @NotBlank(message = "Tier name is required")
    @Size(min = 2, max = 100, message = "Tier name must be between 2 and 100 characters")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
    private BigDecimal price;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    private LocalDateTime salesStartTime;
    private LocalDateTime salesEndTime;

    public CreateTicketTierRequest() {}

    public CreateTicketTierRequest(String name, String description, BigDecimal price, Integer capacity) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.capacity = capacity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public LocalDateTime getSalesStartTime() {
        return salesStartTime;
    }

    public void setSalesStartTime(LocalDateTime salesStartTime) {
        this.salesStartTime = salesStartTime;
    }

    public LocalDateTime getSalesEndTime() {
        return salesEndTime;
    }

    public void setSalesEndTime(LocalDateTime salesEndTime) {
        this.salesEndTime = salesEndTime;
    }
}
