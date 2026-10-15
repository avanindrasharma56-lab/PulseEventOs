package com.eventflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CurrencyConvertRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "From currency is required")
    private String from = "USD";

    @NotBlank(message = "To currency is required")
    private String to = "EUR";

    public CurrencyConvertRequest() {}

    public CurrencyConvertRequest(BigDecimal amount, String from, String to) {
        this.amount = amount;
        this.from = from;
        this.to = to;
    }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
}
