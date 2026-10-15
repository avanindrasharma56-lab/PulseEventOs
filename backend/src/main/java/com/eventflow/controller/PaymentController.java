package com.eventflow.controller;

import com.eventflow.dto.ApiResponse;
import com.eventflow.dto.request.InitiateCheckoutRequest;
import com.eventflow.dto.response.CheckoutSessionResponse;
import com.eventflow.dto.response.PaymentResponse;
import com.eventflow.security.CurrentUser;
import com.eventflow.security.CustomUserDetails;
import com.eventflow.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments & Checkout", description = "Payment Gateway integrations (Stripe, Razorpay, Webhooks)")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/checkout-session")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Initialize Payment Checkout Session", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<CheckoutSessionResponse>> createCheckoutSession(
            @Valid @RequestBody InitiateCheckoutRequest request,
            @CurrentUser CustomUserDetails currentUser) {
        CheckoutSessionResponse response = paymentService.initiateCheckout(request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Checkout session created successfully", response));
    }

    @GetMapping("/{paymentReference}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get Payment Details by Reference", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentStatus(
            @PathVariable String paymentReference,
            @CurrentUser CustomUserDetails currentUser) {
        PaymentResponse response = paymentService.getPaymentByReference(paymentReference, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Payment details retrieved", response));
    }

    @PostMapping("/confirm/{paymentReference}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Confirm Simulated Payment (Instant / Test Mode)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmSimulatedPayment(
            @PathVariable String paymentReference,
            @CurrentUser CustomUserDetails currentUser) {
        PaymentResponse response = paymentService.confirmSimulatedPayment(paymentReference, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed successfully", response));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get Payment History for Order", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getOrderPayments(
            @PathVariable Long orderId,
            @CurrentUser CustomUserDetails currentUser) {
        List<PaymentResponse> responses = paymentService.getOrderPayments(orderId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Order payments retrieved", responses));
    }

    @PostMapping("/webhook/{provider}")
    @Operation(summary = "Inbound Payment Webhook (Stripe / Razorpay / Custom)")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @PathVariable String provider,
            @RequestBody(required = false) String payload,
            @RequestHeader Map<String, String> headers) {
        Map<String, Object> result = paymentService.handleWebhook(provider, payload != null ? payload : "{}", headers);
        return ResponseEntity.ok(result);
    }
}
