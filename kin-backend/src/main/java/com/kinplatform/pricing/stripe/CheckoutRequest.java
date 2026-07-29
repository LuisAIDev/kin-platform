package com.kinplatform.pricing.stripe;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CheckoutRequest {

    @NotNull(message = "Plan ID is required")
    private UUID planId;

    private String successUrl;

    private String cancelUrl;
}
