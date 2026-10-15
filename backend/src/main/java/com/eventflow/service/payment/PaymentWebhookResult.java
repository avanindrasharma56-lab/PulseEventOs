package com.eventflow.service.payment;

import com.eventflow.entity.enums.PaymentStatus;

public class PaymentWebhookResult {
    private boolean verified;
    private String paymentReference;
    private String transactionId;
    private PaymentStatus status;
    private String paymentMethod;
    private String failureReason;
    private String eventType;

    public PaymentWebhookResult() {}

    public static PaymentWebhookResult unverified(String reason) {
        PaymentWebhookResult res = new PaymentWebhookResult();
        res.setVerified(false);
        res.setFailureReason(reason);
        return res;
    }

    public static PaymentWebhookResult success(String paymentReference, String transactionId, String paymentMethod, String eventType) {
        PaymentWebhookResult res = new PaymentWebhookResult();
        res.setVerified(true);
        res.setPaymentReference(paymentReference);
        res.setTransactionId(transactionId);
        res.setStatus(PaymentStatus.COMPLETED);
        res.setPaymentMethod(paymentMethod);
        res.setEventType(eventType);
        return res;
    }

    public static PaymentWebhookResult failed(String paymentReference, String failureReason, String eventType) {
        PaymentWebhookResult res = new PaymentWebhookResult();
        res.setVerified(true);
        res.setPaymentReference(paymentReference);
        res.setStatus(PaymentStatus.FAILED);
        res.setFailureReason(failureReason);
        res.setEventType(eventType);
        return res;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
}
