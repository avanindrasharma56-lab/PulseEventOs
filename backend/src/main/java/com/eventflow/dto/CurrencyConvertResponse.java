package com.eventflow.dto;

import java.math.BigDecimal;

public class CurrencyConvertResponse {
    private BigDecimal originalAmount;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal convertedAmount;
    private BigDecimal exchangeRate;
    private String symbol;
    private String formatted;

    public CurrencyConvertResponse() {}

    public CurrencyConvertResponse(BigDecimal originalAmount, String fromCurrency, String toCurrency,
                                   BigDecimal convertedAmount, BigDecimal exchangeRate, String symbol, String formatted) {
        this.originalAmount = originalAmount;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.convertedAmount = convertedAmount;
        this.exchangeRate = exchangeRate;
        this.symbol = symbol;
        this.formatted = formatted;
    }

    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }

    public String getFromCurrency() { return fromCurrency; }
    public void setFromCurrency(String fromCurrency) { this.fromCurrency = fromCurrency; }

    public String getToCurrency() { return toCurrency; }
    public void setToCurrency(String toCurrency) { this.toCurrency = toCurrency; }

    public BigDecimal getConvertedAmount() { return convertedAmount; }
    public void setConvertedAmount(BigDecimal convertedAmount) { this.convertedAmount = convertedAmount; }

    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getFormatted() { return formatted; }
    public void setFormatted(String formatted) { this.formatted = formatted; }
}
