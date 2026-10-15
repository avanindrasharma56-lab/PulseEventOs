package com.eventflow.service.payment;

import com.eventflow.entity.Order;
import com.eventflow.entity.enums.PaymentProvider;

import java.util.Map;

public interface PaymentGateway {

    PaymentProvider getProvider();

    GatewaySessionResult createSession(Order order, String paymentReference, String successUrl, String cancelUrl);

    PaymentWebhookResult processWebhook(String payload, Map<String, String> headers);
}
