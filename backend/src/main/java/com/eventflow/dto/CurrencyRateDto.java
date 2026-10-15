package com.eventflow.dto;

import java.math.BigDecimal;

public class CurrencyRateDto {
    private String currencyCode;
    private String currencyName;
    private String symbol;
    private BigDecimal rateToUsd;

    public CurrencyRateDto() {}

    public CurrencyRateDto(String currencyCode, String currencyName, String symbol, BigDecimal rateToUsd) {
        this.currencyCode = currencyCode;
        this.currencyName = currencyName;
        this.symbol = symbol;
        this.rateToUsd = rateToUsd;
    }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getCurrencyName() { return currencyName; }
    public void setCurrencyName(String currencyName) { this.currencyName = currencyName; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public BigDecimal getRateToUsd() { return rateToUsd; }
    public void setRateToUsd(BigDecimal rateToUsd) { this.rateToUsd = rateToUsd; }
}
