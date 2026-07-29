package com.kinplatform.pricing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SubscriptionRequest {

    @NotNull(message = "Plan ID is required")
    private UUID planId;
}
