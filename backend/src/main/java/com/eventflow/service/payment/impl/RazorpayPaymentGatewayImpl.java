package com.eventflow.service.payment.impl;

import com.eventflow.entity.Order;
import com.eventflow.entity.enums.PaymentProvider;
import com.eventflow.service.payment.GatewaySessionResult;
import com.eventflow.service.payment.PaymentGateway;
import com.eventflow.service.payment.PaymentWebhookResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Component
public class RazorpayPaymentGatewayImpl implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentGatewayImpl.class);

    @Value("${pulse.payment.razorpay.key-id:rzp_test_pulse_sandbox_key}")
    private String keyId;

    @Value("${pulse.payment.razorpay.webhook-secret:rzp_whsec_pulse_sandbox}")
    private String webhookSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.RAZORPAY;
    }

    @Override
    public GatewaySessionResult createSession(Order order, String paymentReference, String successUrl, String cancelUrl) {
        String orderId = "order_rzp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        String checkoutUrl = (successUrl != null && !successUrl.isBlank())
                ? successUrl + (successUrl.contains("?") ? "&" : "?") + "rzp_order=" + orderId + "&ref=" + paymentReference
                : "https://api.razorpay.com/v1/checkout/" + orderId;

        String rawResponse = String.format("{\"id\":\"%s\",\"entity\":\"order\",\"amount\":%s,\"currency\":\"INR\",\"status\":\"created\"}",
                orderId, order.getTotalAmount().multiply(java.math.BigDecimal.valueOf(100)).toBigInteger());

        log.info("Initialized Razorpay Order Session: {} for Order #{}", orderId, order.getOrderNumber());
        return new GatewaySessionResult(orderId, keyId, checkoutUrl, rawResponse);
    }

    @Override
    public PaymentWebhookResult processWebhook(String payload, Map<String, String> headers) {
        String signatureHeader = headers.getOrDefault("x-razorpay-signature", headers.get("X-Razorpay-Signature"));

        if (webhookSecret != null && !webhookSecret.isBlank() && signatureHeader != null) {
            boolean valid = verifyRazorpaySignature(payload, signatureHeader, webhookSecret);
            if (!valid) {
                log.warn("Razorpay webhook HMAC-SHA256 signature verification failed");
                return PaymentWebhookResult.unverified("Invalid X-Razorpay-Signature header");
            }
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText("payment.captured");
            JsonNode paymentObj = root.path("payload").path("payment").path("entity");

            String paymentRef = paymentObj.path("notes").path("payment_reference").asText(null);
            if (paymentRef == null || paymentRef.isBlank()) {
                paymentRef = root.path("payment_reference").asText(null);
            }

            String transactionId = paymentObj.path("id").asText("pay_rzp_" + UUID.randomUUID().toString().substring(0, 10));
            String method = paymentObj.path("method").asText("upi");

            if ("payment.captured".equalsIgnoreCase(event) || "order.paid".equalsIgnoreCase(event)) {
                return PaymentWebhookResult.success(paymentRef, transactionId, method, event);
            } else if ("payment.failed".equalsIgnoreCase(event)) {
                String error = paymentObj.path("error_description").asText("Transaction failed");
                return PaymentWebhookResult.failed(paymentRef, error, event);
            } else {
                return PaymentWebhookResult.success(paymentRef, transactionId, method, event);
            }
        } catch (Exception e) {
            log.error("Failed to parse Razorpay webhook payload: {}", e.getMessage());
            return PaymentWebhookResult.unverified("Malformed Razorpay webhook JSON: " + e.getMessage());
        }
    }

    private boolean verifyRazorpaySignature(String payload, String signature, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hash);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}
