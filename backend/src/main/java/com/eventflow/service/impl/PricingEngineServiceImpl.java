package com.eventflow.service.impl;

import com.eventflow.dto.*;
import com.eventflow.entity.*;
import com.eventflow.entity.enums.PricingRuleType;
import com.eventflow.exception.BadRequestException;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.*;
import com.eventflow.service.PricingEngineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PricingEngineServiceImpl implements PricingEngineService {

    private final CurrencyRateRepository currencyRepository;
    private final DynamicPricingRuleRepository ruleRepository;
    private final TicketTierRepository ticketTierRepository;
    private final EventRepository eventRepository;

    public PricingEngineServiceImpl(
            CurrencyRateRepository currencyRepository,
            DynamicPricingRuleRepository ruleRepository,
            TicketTierRepository ticketTierRepository,
            EventRepository eventRepository) {
        this.currencyRepository = currencyRepository;
        this.ruleRepository = ruleRepository;
        this.ticketTierRepository = ticketTierRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public List<CurrencyRateDto> getActiveCurrencies() {
        return currencyRepository.findByActiveTrue().stream()
                .map(c -> new CurrencyRateDto(c.getCurrencyCode(), c.getCurrencyName(), c.getSymbol(), c.getRateToUsd()))
                .collect(Collectors.toList());
    }

    @Override
    public CurrencyConvertResponse convert(BigDecimal amount, String from, String to) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Amount must be non-negative");
        }

        CurrencyRate fromRate = currencyRepository.findByCurrencyCodeIgnoreCase(from)
                .orElseThrow(() -> new ResourceNotFoundException("Unsupported currency: " + from));
        CurrencyRate toRate = currencyRepository.findByCurrencyCodeIgnoreCase(to)
                .orElseThrow(() -> new ResourceNotFoundException("Unsupported currency: " + to));

        // Convert from source to USD, then USD to target
        BigDecimal amountInUsd = amount.divide(fromRate.getRateToUsd(), 6, RoundingMode.HALF_UP);
        BigDecimal converted = amountInUsd.multiply(toRate.getRateToUsd()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal crossRate = toRate.getRateToUsd().divide(fromRate.getRateToUsd(), 6, RoundingMode.HALF_UP);

        String formatted = String.format("%s %s", toRate.getSymbol(), converted.toPlainString());

        return new CurrencyConvertResponse(
                amount.setScale(2, RoundingMode.HALF_UP),
                fromRate.getCurrencyCode(),
                toRate.getCurrencyCode(),
                converted,
                crossRate,
                toRate.getSymbol(),
                formatted
        );
    }

    @Override
    public DynamicPriceQuoteDto getPriceQuote(Long tierId, String targetCurrency) {
        TicketTier tier = ticketTierRepository.findById(tierId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket tier not found: " + tierId));

        String currencyCode = (targetCurrency != null && !targetCurrency.isBlank()) ? targetCurrency.toUpperCase() : "USD";
        CurrencyRate targetRate = currencyRepository.findByCurrencyCodeIgnoreCase(currencyCode)
                .orElseGet(() -> currencyRepository.findByCurrencyCodeIgnoreCase("USD").orElse(null));

        if (targetRate == null) {
            targetRate = new CurrencyRate("USD", "US Dollar", "$", BigDecimal.ONE);
        }

        BigDecimal basePrice = tier.getPrice() != null ? tier.getPrice() : BigDecimal.ZERO;
        BigDecimal multiplier = BigDecimal.ONE;
        List<String> appliedRules = new ArrayList<>();
        boolean surgeActive = false;

        // Calculate sold percentage
        int totalQty = tier.getCapacity() != null ? tier.getCapacity() : 0;
        int soldQty = tier.getSoldCount() != null ? tier.getSoldCount() : 0;
        double soldPercent = totalQty > 0 ? ((double) soldQty / totalQty) * 100.0 : 0.0;

        // Check custom rules
        Event event = tier.getEvent();
        List<DynamicPricingRule> rules = event != null ? ruleRepository.findByEventIdAndActiveTrue(event.getId()) : List.of();

        if (!rules.isEmpty()) {
            for (DynamicPricingRule r : rules) {
                if (r.getRuleType() == PricingRuleType.CAPACITY_SURGE) {
                    int thresh = r.getThresholdPercent() != null ? r.getThresholdPercent() : 80;
                    if (soldPercent >= thresh) {
                        multiplier = multiplier.multiply(r.getMultiplier());
                        surgeActive = true;
                        appliedRules.add("Capacity Surge (Sold " + (int) soldPercent + "% >= " + thresh + "%): +" +
                                (r.getMultiplier().subtract(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).intValue()) + "%");
                    }
                } else if (r.getRuleType() == PricingRuleType.EARLY_BIRD && event != null && event.getStartTime() != null) {
                    long daysUntil = ChronoUnit.DAYS.between(LocalDateTime.now(), event.getStartTime());
                    int threshDays = r.getThresholdPercent() != null ? r.getThresholdPercent() : 14;
                    if (daysUntil >= threshDays) {
                        multiplier = multiplier.multiply(r.getMultiplier());
                        appliedRules.add("Early Bird Incentive (" + daysUntil + " days prior): " +
                                (r.getMultiplier().subtract(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).intValue()) + "%");
                    }
                }
            }
        } else {
            // Default built-in dynamic surge rule: sold >= 80% capacity -> +15% surge
            if (soldPercent >= 80.0 && totalQty > 0) {
                BigDecimal defaultSurge = new BigDecimal("1.15");
                multiplier = multiplier.multiply(defaultSurge);
                surgeActive = true;
                appliedRules.add("High Demand Surge (Sold " + (int) soldPercent + "% capacity): +15%");
            }
        }

        BigDecimal effectiveUsd = basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        BigDecimal convertedPrice = effectiveUsd.multiply(targetRate.getRateToUsd()).setScale(2, RoundingMode.HALF_UP);
        String formatted = String.format("%s %s", targetRate.getSymbol(), convertedPrice.toPlainString());

        DynamicPriceQuoteDto quote = new DynamicPriceQuoteDto();
        quote.setTierId(tier.getId());
        quote.setTierName(tier.getName());
        quote.setBasePriceUsd(basePrice.setScale(2, RoundingMode.HALF_UP));
        quote.setEffectivePriceUsd(effectiveUsd);
        quote.setConvertedPrice(convertedPrice);
        quote.setCurrency(targetRate.getCurrencyCode());
        quote.setCurrencySymbol(targetRate.getSymbol());
        quote.setSurgeMultiplier(multiplier.setScale(2, RoundingMode.HALF_UP));
        quote.setSurgeActive(surgeActive);
        quote.setAppliedRules(appliedRules);
        quote.setFormattedPrice(formatted);

        return quote;
    }

    @Override
    @Transactional
    public DynamicPricingRule createRule(Long eventId, PricingRuleType ruleType, Integer thresholdPercent, BigDecimal multiplier) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        if (multiplier == null || multiplier.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Multiplier must be positive");
        }

        DynamicPricingRule rule = new DynamicPricingRule(event, ruleType, thresholdPercent, multiplier);
        return ruleRepository.save(rule);
    }

    @Override
    public List<DynamicPricingRule> getRulesForEvent(Long eventId) {
        return ruleRepository.findByEventIdAndActiveTrue(eventId);
    }
}
