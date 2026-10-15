package com.eventflow.service.payment.impl;

import com.eventflow.entity.Order;
import com.eventflow.entity.enums.PaymentProvider;
import com.eventflow.service.payment.GatewaySessionResult;
import com.eventflow.service.payment.PaymentGateway;
import com.eventflow.service.payment.PaymentWebhookResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class SimulatedPaymentGatewayImpl implements PaymentGateway {

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.SIMULATED;
    }

    @Override
    public GatewaySessionResult createSession(Order order, String paymentReference, String successUrl, String cancelUrl) {
        String sessionId = "sim_sess_" + UUID.randomUUID().toString().substring(0, 16);
        String clientSecret = "sim_secret_" + UUID.randomUUID().toString().substring(0, 16);
        String checkoutUrl = (successUrl != null && !successUrl.isBlank())
                ? successUrl + (successUrl.contains("?") ? "&" : "?") + "session_id=" + sessionId + "&ref=" + paymentReference
                : "/checkout/simulated/" + paymentReference;

        String rawResponse = "{\"status\":\"simulated_ready\",\"session\":\"" + sessionId + "\"}";
        return new GatewaySessionResult(sessionId, clientSecret, checkoutUrl, rawResponse);
    }

    @Override
    public PaymentWebhookResult processWebhook(String payload, Map<String, String> headers) {
        String paymentRef = headers.getOrDefault("payment-reference", "SIM-AUTO");
        String txnId = "sim_txn_" + UUID.randomUUID().toString().substring(0, 10);
        return PaymentWebhookResult.success(paymentRef, txnId, "simulated_instant", "simulated.payment.completed");
    }
}
