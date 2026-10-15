package com.eventflow.service.payment;

import com.eventflow.dto.request.InitiateCheckoutRequest;
import com.eventflow.dto.response.CheckoutSessionResponse;
import com.eventflow.dto.response.PaymentResponse;

import java.util.List;
import java.util.Map;

public interface PaymentService {

    CheckoutSessionResponse initiateCheckout(InitiateCheckoutRequest request, Long currentUserId);

    PaymentResponse getPaymentByReference(String paymentReference, Long currentUserId);

    PaymentResponse confirmSimulatedPayment(String paymentReference, Long currentUserId);

    Map<String, Object> handleWebhook(String providerStr, String payload, Map<String, String> headers);

    List<PaymentResponse> getOrderPayments(Long orderId, Long currentUserId);
}
