package com.kinplatform.common.pricing;

import com.kinplatform.kin.health.documents.port.DocumentStorageQuotaPort;
import com.kinplatform.kin.health.subscription.port.HealthQuotaPort;
import com.kinplatform.common.pricing.dto.PatientSubscriptionStatusResponse;
import com.kinplatform.common.pricing.dto.SubscriptionResponse;
import com.kinplatform.common.pricing.PricingPlanRepository;
import com.kinplatform.common.pricing.SubscriptionStatus;
import com.kinplatform.common.pricing.UserSubscription;
import com.kinplatform.common.pricing.UserSubscriptionRepository;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.pricing.ProductVertical;
import com.kinplatform.common.ai.usage.AiBudgetControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final UserSubscriptionRepository subscriptionRepository;
    private final PricingPlanRepository planRepository;
    private final UserRepository userRepository;
    private final HealthQuotaPort healthQuotaPort;
    private final AiBudgetControlService aiBudgetControlService;
    private final DocumentStorageQuotaPort documentStorageQuotaPort;

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
    @Transactional
    public SubscriptionResponse cancelPatientSubscription(UUID userId) {
        log.info("Cancelling patient subscription for user {}", userId);

        var subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("No active subscription found for user: " + userId));

        var plan = subscription.getPlan();
        if (plan.getVertical() != ProductVertical.SALUD_PERSONAL) {
            throw new IllegalArgumentException("El usuario no tiene una suscripción de paciente");
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setEndDate(OffsetDateTime.now());
        var saved = subscriptionRepository.save(subscription);

        var user = saved.getUser();
        // Revertir al plan FREE de SALUD_PERSONAL
        var freePlan = planRepository.findByCodeAndVertical("FREE", ProductVertical.SALUD_PERSONAL)
                .orElseThrow(() -> new RuntimeException("Plan FREE no encontrado para SALUD_PERSONAL"));
        user.setCurrentPlan(freePlan);
        userRepository.save(user);

        log.info("Patient subscription cancelled for user {}, reverted to FREE plan", userId);
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

    @Override
    @Transactional(readOnly = true)
    public PatientSubscriptionStatusResponse getPatientSubscriptionStatus(UUID userId) {
        log.debug("Fetching patient subscription status for user {}", userId);

        var subscription = subscriptionRepository
                .findByUserIdAndStatusAndEndDateAfter(userId, SubscriptionStatus.ACTIVE, OffsetDateTime.now())
                .orElse(null);

        if (subscription == null) {
            // Usuario sin suscripción activa - devolver estado del plan por defecto (FREE)
            var defaultPlan = planRepository
                    .findByCodeAndVertical("FREE", ProductVertical.SALUD_PERSONAL)
                    .orElseThrow(() -> new IllegalStateException("Plan FREE no encontrado para SALUD_PERSONAL"));

            return PatientSubscriptionStatusResponse.builder()
                    .isActive(false)
                    .planName(defaultPlan.getName())
                    .planCode(defaultPlan.getCode())
                    .planDescription(defaultPlan.getDescription())
                    .maxTriagesPerMonth(defaultPlan.getMaxTriagesPerMonth())
                    .triagesUsed(0)
                    .triagesRemaining(defaultPlan.getMaxTriagesPerMonth())
                    .maxStorageMb(null)
                    .storageUsedMb(0)
                    .storageRemainingMb(null)
                    .aiBudgetUsd(defaultPlan.getAiBudgetUsd())
                    .aiBudgetUsed(BigDecimal.ZERO)
                    .aiBudgetRemaining(defaultPlan.getAiBudgetUsd())
                    .aiLevel("FLASH")
                    .pdfExport(defaultPlan.getPdfExport())
                    .triageSharing(defaultPlan.getTriageSharing())
                    .advancedAI(defaultPlan.getAdvancedAI())
                    .supportLevel(defaultPlan.getSupportLevel().name())
                    .periodStart(OffsetDateTime.now())
                    .periodEnd(OffsetDateTime.now().plusMonths(1))
                    .subscriptionEndDate(null)
                    .build();
        }

        var plan = subscription.getPlan();
        var now = OffsetDateTime.now();

        // Obtener triajes usados del período actual
        int triagesUsed = 0;
        Integer maxTriagesPerMonth = plan.getMaxTriagesPerMonth();
        Integer triagesRemaining = null;

        if (maxTriagesPerMonth != null) {
            triagesUsed = healthQuotaPort.getTriagesUsed(userId);
            triagesRemaining = Math.max(0, maxTriagesPerMonth - triagesUsed);
        }

        // Almacenamiento
        long maxStorageBytes = documentStorageQuotaPort.getStorageLimitBytes(userId);
        Integer maxStorageMb = maxStorageBytes == Long.MAX_VALUE ? null : (int) (maxStorageBytes / (1024 * 1024));
        long storageUsedBytes = documentStorageQuotaPort.getStorageUsedBytes(userId);
        Integer storageUsedMb = (int) (storageUsedBytes / (1024 * 1024));
        Integer storageRemainingMb = maxStorageMb != null ? Math.max(0, maxStorageMb - storageUsedMb) : null;

        // Presupuesto de IA
        var aiUsage = aiBudgetControlService.summary(userId, plan);
        BigDecimal aiBudgetUsd = plan.getAiBudgetUsd();
        BigDecimal aiBudgetUsed = aiUsage.budgetUsed();
        BigDecimal aiBudgetRemaining = aiUsage.budgetRemaining();
        String aiLevel = plan.getAdvancedAI() ? "PRO" : "FLASH";

        // Fechas del período
        var periodStart = subscription.getStartDate();
        var periodEnd = subscription.getEndDate() != null ? subscription.getEndDate() : periodStart.plusMonths(1);

        return PatientSubscriptionStatusResponse.builder()
                .isActive(subscription.getStatus() == SubscriptionStatus.ACTIVE)
                .planName(plan.getName())
                .planCode(plan.getCode())
                .planDescription(plan.getDescription())
                .maxTriagesPerMonth(maxTriagesPerMonth)
                .triagesUsed(triagesUsed)
                .triagesRemaining(triagesRemaining)
                .maxStorageMb(maxStorageMb)
                .storageUsedMb(storageUsedMb)
                .storageRemainingMb(storageRemainingMb)
                .aiBudgetUsd(aiBudgetUsd)
                .aiBudgetUsed(aiBudgetUsed)
                .aiBudgetRemaining(aiBudgetRemaining)
                .aiLevel(aiLevel)
                .pdfExport(plan.getPdfExport())
                .triageSharing(plan.getTriageSharing())
                .advancedAI(plan.getAdvancedAI())
                .supportLevel(plan.getSupportLevel().name())
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .subscriptionEndDate(subscription.getEndDate())
                .build();
    }
}


