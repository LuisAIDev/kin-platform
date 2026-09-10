package com.kinplatform.kin.health.subscription.adapter;

import com.kinplatform.pricing.PricingPlan;
import com.kinplatform.pricing.PricingPlanRepository;
import com.kinplatform.pricing.ProductVertical;
import com.kinplatform.pricing.SubscriptionStatus;
import com.kinplatform.pricing.UserSubscription;
import com.kinplatform.pricing.UserSubscriptionRepository;
import com.kinplatform.kin.health.subscription.port.HealthQuotaPort;
import com.kinplatform.kin.health.triage.port.TriageConsultationRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HealthQuotaPortImpl implements HealthQuotaPort {

    private final UserSubscriptionRepository subscriptionRepository;
    private final PricingPlanRepository planRepository;
    private final UserRepository userRepository;
    private final com.kinplatform.kin.health.triage.port.TriageConsultationRepository triageRepository;

    public HealthQuotaPortImpl(
            UserSubscriptionRepository subscriptionRepository,
            PricingPlanRepository planRepository,
            UserRepository userRepository,
            com.kinplatform.kin.health.triage.port.TriageConsultationRepository triageRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.userRepository = userRepository;
        this.triageRepository = triageRepository;
    }

    @Override
    public Integer getMaxTriagesPerMonth(UUID userId) {
        return resolvePlan(userId).getMaxTriagesPerMonth();
    }

    @Override
    public Integer getTriagesUsed(UUID userId) {
        var subscription = subscriptionRepository
                .findByUserIdAndStatusAndEndDateAfter(userId, SubscriptionStatus.ACTIVE, OffsetDateTime.now())
                .orElse(null);

        if (subscription == null) {
            return 0;
        }

        var periodStart = subscription.getStartDate();
        var periodEnd = subscription.getEndDate() != null
                ? subscription.getEndDate()
                : OffsetDateTime.now().plusMonths(1);

        return triageRepository.countByUserIdAndCreatedAtBetween(userId, periodStart, periodEnd);
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
    public boolean hasEligibleSubscription(
            UUID userId, ProductVertical vertical, SubscriptionStatus... statuses) {
        List<SubscriptionStatus> allowed =
                statuses == null || statuses.length == 0
                        ? List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIAL)
                        : List.of(statuses);
        for (SubscriptionStatus status : allowed) {
            if (subscriptionRepository
                    .findByUserAndPlanVerticalAndStatus(userId, vertical, status)
                    .filter(s -> s.getEndDate() == null || s.getEndDate().isAfter(OffsetDateTime.now()))
                    .isPresent()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resuelve el plan que aplica a un usuario.
     *
     * <p>Si tiene una suscripción {@code ACTIVE} vigente se usa el plan de esa
     * suscripción. Si no, se cae al plan más barato ACTIVO de la vertical que
     * corresponde al rol del usuario (no al plan global de cualquier vertical):
     * un médico sin suscripción debe caer en {@code SALUD_PROFESIONAL} (p. ej.
     * Profesional Trial) y un paciente en {@code SALUD_PERSONAL} (Personal
     * Free), no en el FREE de EMPRESAS.</p>
     */
    private PricingPlan resolvePlan(UUID userId) {
        UserSubscription subscription = subscriptionRepository
                .findByUserIdAndStatusAndEndDateAfter(userId, SubscriptionStatus.ACTIVE, OffsetDateTime.now())
                .orElse(null);
        if (subscription != null) {
            return subscription.getPlan();
        }
        ProductVertical vertical = verticalForRole(userId);
        return planRepository.findFirstByVerticalAndIsActiveTrueOrderByPriceAsc(vertical)
                .orElseThrow(() -> new RuntimeException("No active pricing plan found for vertical " + vertical));
    }

    private ProductVertical verticalForRole(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getRole() == UserRole.PATIENT) {
            return ProductVertical.SALUD_PERSONAL;
        }
        if (user != null && user.getRole() == UserRole.PHYSICIAN) {
            return ProductVertical.SALUD_PROFESIONAL;
        }
        return ProductVertical.EMPRESAS;
    }
}
