package com.eventflow.service.impl;

import com.eventflow.dto.CreatePromoCodeRequest;
import com.eventflow.dto.PromoCodeResponse;
import com.eventflow.dto.ValidatePromoCodeRequest;
import com.eventflow.entity.Event;
import com.eventflow.entity.PromoCode;
import com.eventflow.entity.enums.DiscountType;
import com.eventflow.exception.BadRequestException;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.EventRepository;
import com.eventflow.repository.PromoCodeRepository;
import com.eventflow.service.PromoCodeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PromoCodeServiceImpl implements PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final EventRepository eventRepository;

    public PromoCodeServiceImpl(PromoCodeRepository promoCodeRepository, EventRepository eventRepository) {
        this.promoCodeRepository = promoCodeRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PromoCodeResponse validatePromoCode(ValidatePromoCodeRequest request) {
        PromoCodeResponse response = new PromoCodeResponse();
        response.setCode(request.getCode().trim().toUpperCase());

        PromoCode promo = promoCodeRepository.findByCodeIgnoreCase(request.getCode().trim())
                .orElse(null);

        if (promo == null || !promo.isActive()) {
            response.setValid(false);
            response.setMessage("Invalid or inactive promo code");
            response.setDiscountAmount(BigDecimal.ZERO);
            response.setFinalTotal(request.getOrderTotal());
            return response;
        }

        LocalDateTime now = LocalDateTime.now();
        if (promo.getValidFrom() != null && now.isBefore(promo.getValidFrom())) {
            response.setValid(false);
            response.setMessage("Promo code is not valid yet");
            response.setDiscountAmount(BigDecimal.ZERO);
            response.setFinalTotal(request.getOrderTotal());
            return response;
        }

        if (promo.getValidUntil() != null && now.isAfter(promo.getValidUntil())) {
            response.setValid(false);
            response.setMessage("Promo code has expired");
            response.setDiscountAmount(BigDecimal.ZERO);
            response.setFinalTotal(request.getOrderTotal());
            return response;
        }

        if (promo.getMaxUses() != null && promo.getCurrentUses() >= promo.getMaxUses()) {
            response.setValid(false);
            response.setMessage("Promo code usage limit has been reached");
            response.setDiscountAmount(BigDecimal.ZERO);
            response.setFinalTotal(request.getOrderTotal());
            return response;
        }

        if (promo.getEvent() != null && request.getEventId() != null
                && !promo.getEvent().getId().equals(request.getEventId())) {
            response.setValid(false);
            response.setMessage("This promo code is only valid for a specific event");
            response.setDiscountAmount(BigDecimal.ZERO);
            response.setFinalTotal(request.getOrderTotal());
            return response;
        }

        if (request.getOrderTotal().compareTo(promo.getMinOrderAmount()) < 0) {
            response.setValid(false);
            response.setMessage(String.format("Minimum order amount of $%.2f required to apply this code",
                    promo.getMinOrderAmount()));
            response.setDiscountAmount(BigDecimal.ZERO);
            response.setFinalTotal(request.getOrderTotal());
            return response;
        }

        BigDecimal discount = calculateDiscount(promo, request.getOrderTotal());
        BigDecimal finalTotal = request.getOrderTotal().subtract(discount).max(BigDecimal.ZERO);

        response.setId(promo.getId());
        response.setDescription(promo.getDescription());
        response.setDiscountType(promo.getDiscountType());
        response.setDiscountValue(promo.getDiscountValue());
        response.setDiscountAmount(discount);
        response.setFinalTotal(finalTotal);
        response.setValid(true);
        response.setMessage(String.format("Promo code '%s' applied: $%.2f discount", promo.getCode(), discount));
        return response;
    }

    @Override
    public PromoCode getValidPromoCodeOrThrow(String code, Long eventId, BigDecimal orderAmount) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        PromoCode promo = promoCodeRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BadRequestException("Promo code '" + code + "' not found"));

        if (!promo.isActive()) {
            throw new BadRequestException("Promo code '" + code + "' is inactive");
        }

        LocalDateTime now = LocalDateTime.now();
        if (promo.getValidFrom() != null && now.isBefore(promo.getValidFrom())) {
            throw new BadRequestException("Promo code '" + code + "' is not yet valid");
        }

        if (promo.getValidUntil() != null && now.isAfter(promo.getValidUntil())) {
            throw new BadRequestException("Promo code '" + code + "' has expired");
        }

        if (promo.getMaxUses() != null && promo.getCurrentUses() >= promo.getMaxUses()) {
            throw new BadRequestException("Promo code '" + code + "' usage quota has been exhausted");
        }

        if (promo.getEvent() != null && eventId != null && !promo.getEvent().getId().equals(eventId)) {
            throw new BadRequestException("Promo code '" + code + "' is not valid for this event");
        }

        if (orderAmount.compareTo(promo.getMinOrderAmount()) < 0) {
            throw new BadRequestException(String.format("Minimum order total of $%.2f required for promo code '%s'",
                    promo.getMinOrderAmount(), code));
        }

        return promo;
    }

    @Override
    public void incrementPromoUsage(Long promoId) {
        if (promoId != null) {
            promoCodeRepository.findById(promoId).ifPresent(p -> {
                p.setCurrentUses(p.getCurrentUses() + 1);
                promoCodeRepository.save(p);
            });
        }
    }

    @Override
    public PromoCodeResponse createPromoCode(CreatePromoCodeRequest request) {
        String cleanCode = request.getCode().trim().toUpperCase();
        if (promoCodeRepository.existsByCodeIgnoreCase(cleanCode)) {
            throw new BadRequestException("Promo code '" + cleanCode + "' already exists");
        }

        PromoCode promo = new PromoCode();
        promo.setCode(cleanCode);
        promo.setDescription(request.getDescription());
        promo.setDiscountType(request.getDiscountType());
        promo.setDiscountValue(request.getDiscountValue());
        promo.setMaxUses(request.getMaxUses());
        promo.setMinOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : BigDecimal.ZERO);
        promo.setValidFrom(request.getValidFrom());
        promo.setValidUntil(request.getValidUntil());
        promo.setActive(true);

        if (request.getEventId() != null) {
            Event event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event", "id", request.getEventId()));
            promo.setEvent(event);
        }

        PromoCode saved = promoCodeRepository.save(promo);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromoCodeResponse> getAllPromoCodes() {
        return promoCodeRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private BigDecimal calculateDiscount(PromoCode promo, BigDecimal orderTotal) {
        if (promo.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal percentage = promo.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            return orderTotal.multiply(percentage).setScale(2, RoundingMode.HALF_UP);
        } else {
            return promo.getDiscountValue().min(orderTotal).setScale(2, RoundingMode.HALF_UP);
        }
    }

    private PromoCodeResponse mapToResponse(PromoCode promo) {
        PromoCodeResponse res = new PromoCodeResponse();
        res.setId(promo.getId());
        res.setCode(promo.getCode());
        res.setDescription(promo.getDescription());
        res.setDiscountType(promo.getDiscountType());
        res.setDiscountValue(promo.getDiscountValue());
        res.setValid(promo.isActive());
        res.setMessage(promo.isActive() ? "Active" : "Inactive");
        return res;
    }
}
