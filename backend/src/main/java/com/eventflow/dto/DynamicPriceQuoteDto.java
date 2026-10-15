package com.eventflow.dto;

import java.math.BigDecimal;
import java.util.List;

public class DynamicPriceQuoteDto {
    private Long tierId;
    private String tierName;
    private BigDecimal basePriceUsd;
    private BigDecimal effectivePriceUsd;
    private BigDecimal convertedPrice;
    private String currency;
    private String currencySymbol;
    private BigDecimal surgeMultiplier;
    private boolean surgeActive;
    private List<String> appliedRules;
    private String formattedPrice;

    public DynamicPriceQuoteDto() {}

    public Long getTierId() { return tierId; }
    public void setTierId(Long tierId) { this.tierId = tierId; }

    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }

    public BigDecimal getBasePriceUsd() { return basePriceUsd; }
    public void setBasePriceUsd(BigDecimal basePriceUsd) { this.basePriceUsd = basePriceUsd; }

    public BigDecimal getEffectivePriceUsd() { return effectivePriceUsd; }
    public void setEffectivePriceUsd(BigDecimal effectivePriceUsd) { this.effectivePriceUsd = effectivePriceUsd; }

    public BigDecimal getConvertedPrice() { return convertedPrice; }
    public void setConvertedPrice(BigDecimal convertedPrice) { this.convertedPrice = convertedPrice; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getCurrencySymbol() { return currencySymbol; }
    public void setCurrencySymbol(String currencySymbol) { this.currencySymbol = currencySymbol; }

    public BigDecimal getSurgeMultiplier() { return surgeMultiplier; }
    public void setSurgeMultiplier(BigDecimal surgeMultiplier) { this.surgeMultiplier = surgeMultiplier; }

    public boolean isSurgeActive() { return surgeActive; }
    public void setSurgeActive(boolean surgeActive) { this.surgeActive = surgeActive; }

    public List<String> getAppliedRules() { return appliedRules; }
    public void setAppliedRules(List<String> appliedRules) { this.appliedRules = appliedRules; }

    public String getFormattedPrice() { return formattedPrice; }
    public void setFormattedPrice(String formattedPrice) { this.formattedPrice = formattedPrice; }
}
