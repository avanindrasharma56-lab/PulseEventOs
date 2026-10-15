package com.eventflow.service.payment;

public class GatewaySessionResult {
    private String sessionId;
    private String clientSecret;
    private String checkoutUrl;
    private String rawResponse;

    public GatewaySessionResult(String sessionId, String clientSecret, String checkoutUrl, String rawResponse) {
        this.sessionId = sessionId;
        this.clientSecret = clientSecret;
        this.checkoutUrl = checkoutUrl;
        this.rawResponse = rawResponse;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public String getRawResponse() {
        return rawResponse;
    }
}
