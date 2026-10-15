package com.eventflow.service;

import com.eventflow.dto.*;
import com.eventflow.entity.DynamicPricingRule;
import com.eventflow.entity.enums.PricingRuleType;

import java.math.BigDecimal;
import java.util.List;

public interface PricingEngineService {
    List<CurrencyRateDto> getActiveCurrencies();
    CurrencyConvertResponse convert(BigDecimal amount, String from, String to);
    DynamicPriceQuoteDto getPriceQuote(Long tierId, String targetCurrency);
    DynamicPricingRule createRule(Long eventId, PricingRuleType ruleType, Integer thresholdPercent, BigDecimal multiplier);
    List<DynamicPricingRule> getRulesForEvent(Long eventId);
}
