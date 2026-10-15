package com.eventflow.controller;

import com.eventflow.dto.ApiResponse;
import com.eventflow.dto.CreatePromoCodeRequest;
import com.eventflow.dto.PromoCodeResponse;
import com.eventflow.dto.ValidatePromoCodeRequest;
import com.eventflow.service.PromoCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promo-codes")
@Tag(name = "Promo & Discount Codes", description = "Endpoints for validating and configuring discount promo codes")
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    public PromoCodeController(PromoCodeService promoCodeService) {
        this.promoCodeService = promoCodeService;
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate promo code against order amount")
    public ResponseEntity<ApiResponse<PromoCodeResponse>> validatePromoCode(@Valid @RequestBody ValidatePromoCodeRequest request) {
        PromoCodeResponse response = promoCodeService.validatePromoCode(request);
        return ResponseEntity.ok(ApiResponse.success("Promo code checked", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "List all promo codes (Organizers and Admins)")
    public ResponseEntity<ApiResponse<List<PromoCodeResponse>>> getAllPromoCodes() {
        List<PromoCodeResponse> list = promoCodeService.getAllPromoCodes();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Create a new promo discount code")
    public ResponseEntity<ApiResponse<PromoCodeResponse>> createPromoCode(@Valid @RequestBody CreatePromoCodeRequest request) {
        PromoCodeResponse response = promoCodeService.createPromoCode(request);
        return new ResponseEntity<>(ApiResponse.success("Promo code created successfully", response), HttpStatus.CREATED);
    }
}
