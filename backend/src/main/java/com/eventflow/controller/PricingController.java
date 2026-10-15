package com.eventflow.controller;

import com.eventflow.dto.*;
import com.eventflow.entity.DynamicPricingRule;
import com.eventflow.entity.enums.PricingRuleType;
import com.eventflow.service.PricingEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Dynamic Pricing & Multi-Currency Engine", description = "Real-time surge pricing, early bird discounts, and global multi-currency exchange (USD, EUR, GBP, INR)")
public class PricingController {

    private final PricingEngineService pricingEngineService;

    public PricingController(PricingEngineService pricingEngineService) {
        this.pricingEngineService = pricingEngineService;
    }

    @GetMapping("/pricing/currencies")
    @Operation(summary = "Get list of all supported global currencies with exchange rates")
    public ResponseEntity<ApiResponse<List<CurrencyRateDto>>> getCurrencies() {
        List<CurrencyRateDto> list = pricingEngineService.getActiveCurrencies();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/pricing/convert")
    @Operation(summary = "Convert arbitrary amount between supported currencies")
    public ResponseEntity<ApiResponse<CurrencyConvertResponse>> convert(
            @Valid @RequestBody CurrencyConvertRequest request) {
        CurrencyConvertResponse response = pricingEngineService.convert(request.getAmount(), request.getFrom(), request.getTo());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/pricing/tiers/{tierId}/quote")
    @Operation(summary = "Calculate dynamic surge price and multi-currency quote for a ticket tier")
    public ResponseEntity<ApiResponse<DynamicPriceQuoteDto>> getPriceQuote(
            @PathVariable Long tierId,
            @RequestParam(required = false, defaultValue = "USD") String currency) {
        DynamicPriceQuoteDto quote = pricingEngineService.getPriceQuote(tierId, currency);
        return ResponseEntity.ok(ApiResponse.success(quote));
    }

    @PostMapping("/events/{eventId}/pricing-rules")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Configure custom dynamic pricing rule for an event (Organizer/Admin)")
    public ResponseEntity<ApiResponse<DynamicPricingRule>> createPricingRule(
            @PathVariable Long eventId,
            @RequestParam PricingRuleType ruleType,
            @RequestParam(required = false) Integer thresholdPercent,
            @RequestParam BigDecimal multiplier) {
        DynamicPricingRule rule = pricingEngineService.createRule(eventId, ruleType, thresholdPercent, multiplier);
        return ResponseEntity.ok(ApiResponse.success("Dynamic pricing rule registered", rule));
    }

    @GetMapping("/events/{eventId}/pricing-rules")
    @Operation(summary = "Get all active dynamic pricing rules for an event")
    public ResponseEntity<ApiResponse<List<DynamicPricingRule>>> getEventPricingRules(
            @PathVariable Long eventId) {
        List<DynamicPricingRule> rules = pricingEngineService.getRulesForEvent(eventId);
        return ResponseEntity.ok(ApiResponse.success(rules));
    }
}
