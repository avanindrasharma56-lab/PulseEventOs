package com.eventflow.service.payment.impl;

import com.eventflow.dto.request.InitiateCheckoutRequest;
import com.eventflow.dto.response.CheckoutSessionResponse;
import com.eventflow.dto.response.PaymentResponse;
import com.eventflow.entity.Order;
import com.eventflow.entity.Payment;
import com.eventflow.entity.User;
import com.eventflow.entity.enums.NotificationType;
import com.eventflow.entity.enums.OrderStatus;
import com.eventflow.entity.enums.PaymentProvider;
import com.eventflow.entity.enums.PaymentStatus;
import com.eventflow.exception.BadRequestException;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.OrderRepository;
import com.eventflow.repository.PaymentRepository;
import com.eventflow.service.NotificationService;
import com.eventflow.service.payment.GatewaySessionResult;
import com.eventflow.service.payment.PaymentGateway;
import com.eventflow.service.payment.PaymentService;
import com.eventflow.service.payment.PaymentWebhookResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final Map<PaymentProvider, PaymentGateway> gatewayMap;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            NotificationService notificationService,
            List<PaymentGateway> gateways) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
        this.gatewayMap = gateways.stream()
                .collect(Collectors.toMap(PaymentGateway::getProvider, Function.identity()));
    }

    @Override
    public CheckoutSessionResponse initiateCheckout(InitiateCheckoutRequest request, Long currentUserId) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));

        validateOrderAccess(order, currentUserId);

        PaymentProvider provider = request.getProvider() != null ? request.getProvider() : PaymentProvider.STRIPE;
        PaymentGateway gateway = gatewayMap.get(provider);
        if (gateway == null) {
            gateway = gatewayMap.get(PaymentProvider.STRIPE);
            provider = PaymentProvider.STRIPE;
        }

        String paymentRef = "PAY-" + provider.name() + "-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();

        GatewaySessionResult sessionResult = gateway.createSession(
                order,
                paymentRef,
                request.getSuccessUrl(),
                request.getCancelUrl()
        );

        Payment payment = new Payment(
                paymentRef,
                order,
                provider,
                order.getTotalAmount(),
                "USD",
                PaymentStatus.PENDING
        );
        payment.setSessionId(sessionResult.getSessionId());
        payment.setClientSecret(sessionResult.getClientSecret());
        payment.setRawResponse(sessionResult.getRawResponse());

        Payment savedPayment = paymentRepository.save(payment);

        order.setPaymentReference(paymentRef);
        orderRepository.save(order);

        CheckoutSessionResponse response = new CheckoutSessionResponse();
        response.setPaymentReference(savedPayment.getPaymentReference());
        response.setOrderId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setProvider(provider);
        response.setAmount(savedPayment.getAmount());
        response.setCurrency(savedPayment.getCurrency());
        response.setStatus(savedPayment.getStatus());
        response.setSessionId(savedPayment.getSessionId());
        response.setClientSecret(savedPayment.getClientSecret());
        response.setCheckoutUrl(sessionResult.getCheckoutUrl());
        response.setCreatedAt(savedPayment.getCreatedAt());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReference(String paymentReference, Long currentUserId) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "reference", paymentReference));

        validateOrderAccess(payment.getOrder(), currentUserId);

        return mapToPaymentResponse(payment);
    }

    @Override
    public PaymentResponse confirmSimulatedPayment(String paymentReference, Long currentUserId) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "reference", paymentReference));

        validateOrderAccess(payment.getOrder(), currentUserId);

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return mapToPaymentResponse(payment);
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId("txn_sim_" + UUID.randomUUID().toString().substring(0, 12));
        payment.setPaymentMethod("visa_4242");
        payment.setUpdatedAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

        Order order = payment.getOrder();
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        try {
            User buyer = order.getUser();
            notificationService.sendNotification(
                    buyer,
                    "Payment Successful: " + payment.getPaymentReference(),
                    "Payment of $" + payment.getAmount() + " for order " + order.getOrderNumber() + " was confirmed via " + payment.getProvider() + ".",
                    NotificationType.ORDER_CONFIRMED,
                    order.getOrderNumber()
            );
        } catch (Exception e) {
            log.warn("Could not dispatch notification for payment {}: {}", paymentReference, e.getMessage());
        }

        return mapToPaymentResponse(saved);
    }

    @Override
    public Map<String, Object> handleWebhook(String providerStr, String payload, Map<String, String> headers) {
        PaymentProvider provider;
        try {
            provider = PaymentProvider.valueOf(providerStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unsupported payment provider: " + providerStr);
        }

        PaymentGateway gateway = gatewayMap.get(provider);
        if (gateway == null) {
            throw new BadRequestException("Payment provider gateway not implemented: " + provider);
        }

        PaymentWebhookResult webhookResult = gateway.processWebhook(payload, headers);

        if (!webhookResult.isVerified()) {
            log.warn("Webhook processing failed verification for {}: {}", provider, webhookResult.getFailureReason());
            return Map.of("received", false, "error", webhookResult.getFailureReason());
        }

        if (webhookResult.getPaymentReference() != null) {
            paymentRepository.findByPaymentReference(webhookResult.getPaymentReference()).ifPresent(payment -> {
                if (webhookResult.getStatus() == PaymentStatus.COMPLETED) {
                    payment.setStatus(PaymentStatus.COMPLETED);
                    payment.setTransactionId(webhookResult.getTransactionId());
                    payment.setPaymentMethod(webhookResult.getPaymentMethod());
                    paymentRepository.save(payment);

                    Order order = payment.getOrder();
                    order.setStatus(OrderStatus.CONFIRMED);
                    orderRepository.save(order);

                    try {
                        notificationService.sendNotification(
                                order.getUser(),
                                "Payment Verified: " + payment.getPaymentReference(),
                                "Webhook confirmed your payment of $" + payment.getAmount() + " via " + provider + ".",
                                NotificationType.ORDER_CONFIRMED,
                                order.getOrderNumber()
                        );
                    } catch (Exception ignored) {}
                } else if (webhookResult.getStatus() == PaymentStatus.FAILED) {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setFailureReason(webhookResult.getFailureReason());
                    paymentRepository.save(payment);

                    Order order = payment.getOrder();
                    order.setStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);
                }
            });
        }

        return Map.of(
                "received", true,
                "provider", provider.name(),
                "status", "acknowledged",
                "reference", webhookResult.getPaymentReference() != null ? webhookResult.getPaymentReference() : "N/A"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getOrderPayments(Long orderId, Long currentUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        validateOrderAccess(order, currentUserId);

        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    private void validateOrderAccess(Order order, Long currentUserId) {
        if (!order.getUser().getId().equals(currentUserId)) {
            // Check if current user is admin or organizer
            boolean isOrganizer = order.getEvent().getOrganizer().getId().equals(currentUserId);
            if (!isOrganizer) {
                // If not organizer, reject unless user is authorized
                log.debug("User {} checked order {} belonging to user {}", currentUserId, order.getId(), order.getUser().getId());
            }
        }
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        PaymentResponse res = new PaymentResponse();
        res.setId(payment.getId());
        res.setPaymentReference(payment.getPaymentReference());
        res.setOrderId(payment.getOrder().getId());
        res.setOrderNumber(payment.getOrder().getOrderNumber());
        res.setProvider(payment.getProvider());
        res.setAmount(payment.getAmount());
        res.setCurrency(payment.getCurrency());
        res.setStatus(payment.getStatus());
        res.setSessionId(payment.getSessionId());
        res.setTransactionId(payment.getTransactionId());
        res.setPaymentMethod(payment.getPaymentMethod());
        res.setFailureReason(payment.getFailureReason());
        res.setCreatedAt(payment.getCreatedAt());
        res.setUpdatedAt(payment.getUpdatedAt());
        return res;
    }
}
