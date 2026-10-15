package com.eventflow.service;

import com.eventflow.dto.CreatePromoCodeRequest;
import com.eventflow.dto.PromoCodeResponse;
import com.eventflow.dto.ValidatePromoCodeRequest;
import com.eventflow.entity.PromoCode;

import java.math.BigDecimal;
import java.util.List;

public interface PromoCodeService {
    PromoCodeResponse validatePromoCode(ValidatePromoCodeRequest request);
    PromoCode getValidPromoCodeOrThrow(String code, Long eventId, BigDecimal orderAmount);
    void incrementPromoUsage(Long promoId);
    PromoCodeResponse createPromoCode(CreatePromoCodeRequest request);
    List<PromoCodeResponse> getAllPromoCodes();
}
