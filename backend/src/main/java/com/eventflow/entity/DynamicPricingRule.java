package com.eventflow.entity;

import com.eventflow.entity.enums.PricingRuleType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dynamic_pricing_rules")
public class DynamicPricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 50)
    private PricingRuleType ruleType;

    @Column(name = "threshold_percent")
    private Integer thresholdPercent;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal multiplier = BigDecimal.ONE;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public DynamicPricingRule() {}

    public DynamicPricingRule(Event event, PricingRuleType ruleType, Integer thresholdPercent, BigDecimal multiplier) {
        this.event = event;
        this.ruleType = ruleType;
        this.thresholdPercent = thresholdPercent;
        this.multiplier = multiplier;
        this.active = true;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public Long getEventId() { return event != null ? event.getId() : null; }

    public PricingRuleType getRuleType() { return ruleType; }
    public void setRuleType(PricingRuleType ruleType) { this.ruleType = ruleType; }

    public Integer getThresholdPercent() { return thresholdPercent; }
    public void setThresholdPercent(Integer thresholdPercent) { this.thresholdPercent = thresholdPercent; }

    public BigDecimal getMultiplier() { return multiplier; }
    public void setMultiplier(BigDecimal multiplier) { this.multiplier = multiplier; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
