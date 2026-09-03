package com.kinplatform.kin.health.subscription.adapter;

import com.kinplatform.pricing.PricingPlan;
import com.kinplatform.pricing.PricingPlanRepository;
import com.kinplatform.pricing.SubscriptionStatus;
import com.kinplatform.pricing.UserSubscription;
import com.kinplatform.pricing.UserSubscriptionRepository;
import com.kinplatform.kin.health.subscription.port.HealthQuotaPort;
import com.kinplatform.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HealthQuotaPortImpl implements HealthQuotaPort {

    private final UserSubscriptionRepository subscriptionRepository;
    private final PricingPlanRepository planRepository;
    private final UserRepository userRepository;

    public HealthQuotaPortImpl(
            UserSubscriptionRepository subscriptionRepository,
            PricingPlanRepository planRepository,
            UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Integer getMaxTriagesPerMonth(UUID userId) {
        return resolvePlan(userId).getMaxTriagesPerMonth();
    }

    @Override
    public Integer getMaxPatients(UUID physicianId) {
        return resolvePlan(physicianId).getMaxPatients();
    }

    @Override
    public Integer getTrialDays(UUID userId) {
        return resolvePlan(userId).getTrialDays();
    }

    @Override
    public boolean hasActiveSubscription(UUID userId, String vertical) {
        UserSubscription subscription = subscriptionRepository
                .findByUserIdAndVerticalAndStatus(userId, vertical, SubscriptionStatus.ACTIVE)
                .orElse(null);
        return subscription != null;
    }

    private PricingPlan resolvePlan(UUID userId) {
        UserSubscription subscription = subscriptionRepository
                .findByUserIdAndStatusAndEndDateAfter(userId, SubscriptionStatus.ACTIVE, OffsetDateTime.now())
                .orElse(null);
        if (subscription != null) {
            return subscription.getPlan();
        }
        return planRepository.findFirstByIsActiveTrueOrderByPriceAsc()
                .orElseThrow(() -> new RuntimeException("No active pricing plan found"));
    }
}