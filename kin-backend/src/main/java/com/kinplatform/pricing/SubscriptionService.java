package com.kinplatform.pricing;

import com.kinplatform.pricing.dto.SubscriptionResponse;

import java.util.UUID;

public interface SubscriptionService {

    SubscriptionResponse subscribe(UUID userId, UUID planId);

    SubscriptionResponse cancelSubscription(UUID userId);

    SubscriptionResponse getCurrentSubscription(UUID userId);

    boolean hasAvailableMessages(UUID userId);

    void incrementMessagesUsed(UUID userId);

    void resetMonthlyUsage();
}
