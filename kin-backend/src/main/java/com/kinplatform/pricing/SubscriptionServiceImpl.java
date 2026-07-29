package com.kinplatform.pricing;

import com.kinplatform.pricing.dto.SubscriptionResponse;
import com.kinplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final UserSubscriptionRepository subscriptionRepository;
    private final PricingPlanRepository planRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SubscriptionResponse subscribe(UUID userId, UUID planId) {
        log.info("User {} subscribing to plan {}", userId, planId);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        var plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Pricing plan not found: " + planId));

        if (!plan.getIsActive()) {
            throw new IllegalArgumentException("Pricing plan is not active: " + planId);
        }

        if (plan.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException(
                    "Paid plans require payment. Use /stripe/create-checkout-session instead.");
        }

        subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .ifPresent(s -> {
                    throw new IllegalArgumentException("User already has an active subscription");
                });

        var now = OffsetDateTime.now();
        var subscription = UserSubscription.builder()
                .user(user)
                .plan(plan)
                .startDate(now)
                .status(SubscriptionStatus.ACTIVE)
                .messagesUsed(0)
                .lastResetDate(now)
                .build();

        var saved = subscriptionRepository.save(subscription);

        user.setCurrentPlan(plan);
        user.setSubscription(saved);
        userRepository.save(user);

        log.info("User {} subscribed to free plan {} successfully", userId, planId);
        return SubscriptionResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public SubscriptionResponse startTrial(UUID userId, UUID planId) {
        log.info("User {} starting trial for plan {}", userId, planId);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        var plan = planRepository.findById(planId)
                .orElseThrow(() -> new PlanNotFoundException("Pricing plan not found: " + planId));

        if (!plan.getIsActive()) {
            throw new IllegalArgumentException("Pricing plan is not active: " + planId);
        }

        subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .ifPresent(s -> {
                    throw new IllegalArgumentException("User already has an active subscription");
                });

        var now = OffsetDateTime.now();
        var trialEnd = now.plusDays(14);

        var subscription = UserSubscription.builder()
                .user(user)
                .plan(plan)
                .startDate(now)
                .endDate(trialEnd)
                .status(SubscriptionStatus.TRIAL)
                .messagesUsed(0)
                .lastResetDate(now)
                .build();

        var saved = subscriptionRepository.save(subscription);

        user.setCurrentPlan(plan);
        user.setSubscription(saved);
        userRepository.save(user);

        log.info("User {} started trial for plan {} until {}", userId, planId, trialEnd);
        return SubscriptionResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public SubscriptionResponse cancelSubscription(UUID userId) {
        log.info("Cancelling subscription for user {}", userId);

        var subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("No active subscription found for user: " + userId));

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setEndDate(OffsetDateTime.now());
        var saved = subscriptionRepository.save(subscription);

        var user = saved.getUser();
        user.setCurrentPlan(null);
        userRepository.save(user);

        log.info("Subscription cancelled for user {}", userId);
        return SubscriptionResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getCurrentSubscription(UUID userId) {
        log.debug("Fetching current subscription for user {}", userId);

        var subscription = subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new IllegalArgumentException("No subscription found for user: " + userId));

        return SubscriptionResponse.fromEntity(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAvailableMessages(UUID userId) {
        var subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElse(null);

        if (subscription == null) {
            return false;
        }

        var plan = subscription.getPlan();
        if (plan.getMessagesPerMonth() == null) {
            return true;
        }

        return subscription.getMessagesUsed() < plan.getMessagesPerMonth();
    }

    @Override
    @Transactional
    public void incrementMessagesUsed(UUID userId) {
        var subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("No active subscription found for user: " + userId));

        var plan = subscription.getPlan();
        if (plan.getMessagesPerMonth() != null
                && subscription.getMessagesUsed() >= plan.getMessagesPerMonth()) {
            throw new IllegalStateException("Monthly message limit reached");
        }

        subscription.setMessagesUsed(subscription.getMessagesUsed() + 1);
        subscriptionRepository.save(subscription);
        log.debug("Incremented messages used for user {}: {}/{}",
                userId, subscription.getMessagesUsed(),
                plan.getMessagesPerMonth() != null ? plan.getMessagesPerMonth() : "unlimited");
    }

    @Override
    @Transactional
    public void resetMonthlyUsage() {
        log.info("Resetting monthly message usage for all active subscriptions");
        var now = OffsetDateTime.now();
        var activeSubscriptions = subscriptionRepository.findAll();

        for (var sub : activeSubscriptions) {
            if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
                sub.setMessagesUsed(0);
                sub.setLastResetDate(now);
                subscriptionRepository.save(sub);
            }
        }
        log.info("Monthly usage reset completed for {} subscriptions", activeSubscriptions.size());
    }
}
