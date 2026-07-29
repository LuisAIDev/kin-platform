package com.kinplatform.pricing.dto;

import com.kinplatform.pricing.SubscriptionStatus;
import com.kinplatform.pricing.UserSubscription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionResponse {

    private UUID id;
    private UUID userId;
    private PricingPlanResponse plan;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private SubscriptionStatus status;
    private Integer messagesUsed;
    private Integer messagesPerMonth;
    private OffsetDateTime lastResetDate;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static SubscriptionResponse fromEntity(UserSubscription subscription) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUser().getId())
                .plan(PricingPlanResponse.fromEntity(subscription.getPlan()))
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .status(subscription.getStatus())
                .messagesUsed(subscription.getMessagesUsed())
                .messagesPerMonth(subscription.getPlan().getMessagesPerMonth())
                .lastResetDate(subscription.getLastResetDate())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }
}
