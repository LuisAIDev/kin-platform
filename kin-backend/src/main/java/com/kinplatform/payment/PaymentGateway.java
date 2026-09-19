package com.kinplatform.payment;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGateway {

    CheckoutSession createCheckoutSession(CreateCheckoutRequest request);

    void handleWebhook(String payload, String signature);

    String getGatewayCode();

    record CreateCheckoutRequest(
            UUID userId,
            UUID planId,
            String planCode,
            BigDecimal amount,
            String currency,
            String successUrl,
            String cancelUrl,
            String vertical
    ) {}

    record CheckoutSession(
            String sessionId,
            String url
    ) {}
}