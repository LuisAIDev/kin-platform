package com.kinplatform.kin.health.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.kinplatform.pricing.PricingPlan;
import com.kinplatform.pricing.PricingPlanRepository;
import com.kinplatform.pricing.ProductVertical;
import com.kinplatform.pricing.SubscriptionStatus;
import com.kinplatform.pricing.UserSubscription;
import com.kinplatform.pricing.UserSubscriptionRepository;
import com.kinplatform.kin.health.subscription.adapter.HealthQuotaPortImpl;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HealthQuotaPortImplTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PHYSICIAN_ID = UUID.randomUUID();
    private static final UUID PLAN_ID = UUID.randomUUID();

    @Mock
    private UserSubscriptionRepository subscriptionRepository;

    @Mock
    private PricingPlanRepository planRepository;

    @Mock
    private UserRepository userRepository;

    private HealthQuotaPortImpl quotaPort;

    @BeforeEach
    void setUp() {
        quotaPort = new HealthQuotaPortImpl(subscriptionRepository, planRepository, userRepository);
    }

    private PricingPlan paidHealthPlan(Integer maxTriages, Integer maxPatients, Integer trialDays) {
        return PricingPlan.builder()
                .id(PLAN_ID)
                .code("SALUD_PERSONAL_PLUS")
                .vertical(com.kinplatform.pricing.ProductVertical.SALUD_PERSONAL)
                .name("Health Plus")
                .price(BigDecimal.valueOf(29.99))
                .features("[]")
                .maxTriagesPerMonth(maxTriages)
                .maxPatients(maxPatients)
                .trialDays(trialDays)
                .isActive(true)
                .build();
    }

    private UserSubscription activeSubscription(PricingPlan plan) {
        return UserSubscription.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(USER_ID).role(UserRole.PHYSICIAN).build())
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .endDate(OffsetDateTime.now().plusMonths(1))
                .build();
    }

    @Test
    void getMaxTriagesPerMonth_suscripcionActiva_devuelveLimite() {
        var plan = paidHealthPlan(10, 50, 30);
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(
                eq(USER_ID), eq(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(Optional.of(activeSubscription(plan)));

        assertEquals(10, quotaPort.getMaxTriagesPerMonth(USER_ID));
    }

    @Test
    void getMaxTriagesPerMonth_sinSuscripcion_devuelvePlanDefault() {
        var defaultPlan = paidHealthPlan(5, 20, 14);
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(
                eq(USER_ID), eq(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(User.builder().id(USER_ID).role(UserRole.PATIENT).build()));
        when(planRepository.findFirstByVerticalAndIsActiveTrueOrderByPriceAsc(ProductVertical.SALUD_PERSONAL))
                .thenReturn(Optional.of(defaultPlan));

        assertEquals(5, quotaPort.getMaxTriagesPerMonth(USER_ID));
    }

    @Test
    void getMaxTriagesPerMonth_planIlimitado_devuelveNull() {
        var plan = paidHealthPlan(null, 50, 30);
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(
                eq(USER_ID), eq(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(Optional.of(activeSubscription(plan)));

        assertNull(quotaPort.getMaxTriagesPerMonth(USER_ID));
    }

    @Test
    void getMaxPatients_medicoConSuscripcion_devuelveLimite() {
        var plan = paidHealthPlan(10, 100, 30);
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(
                eq(PHYSICIAN_ID), eq(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(Optional.of(activeSubscription(plan)));

        assertEquals(100, quotaPort.getMaxPatients(PHYSICIAN_ID));
    }

    @Test
    void getMaxPatients_planIlimitado_devuelveNull() {
        var plan = paidHealthPlan(10, null, 30);
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(
                eq(PHYSICIAN_ID), eq(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(Optional.of(activeSubscription(plan)));

        assertNull(quotaPort.getMaxPatients(PHYSICIAN_ID));
    }

    @Test
    void getTrialDays_suscripcionActiva_devuelveDias() {
        var plan = paidHealthPlan(10, 50, 30);
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(
                eq(USER_ID), eq(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(Optional.of(activeSubscription(plan)));

        assertEquals(30, quotaPort.getTrialDays(USER_ID));
    }

    @Test
    void getTrialDays_sinTrial_devuelveNull() {
        var plan = paidHealthPlan(10, 50, null);
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(
                eq(USER_ID), eq(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(Optional.of(activeSubscription(plan)));

        assertNull(quotaPort.getTrialDays(USER_ID));
    }

    @Test
    void getTrialDays_sinSuscripcion_devuelvePlanDefault() {
        var defaultPlan = paidHealthPlan(5, 20, 14);
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(
                eq(USER_ID), eq(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(User.builder().id(USER_ID).role(UserRole.PHYSICIAN).build()));
        when(planRepository.findFirstByVerticalAndIsActiveTrueOrderByPriceAsc(ProductVertical.SALUD_PROFESIONAL))
                .thenReturn(Optional.of(defaultPlan));

        assertEquals(14, quotaPort.getTrialDays(USER_ID));
    }

    @Test
    void getMaxTriagesPerMonth_suscripcionExpirada_devuelvePlanDefault() {
        var defaultPlan = paidHealthPlan(5, 20, 14);
        var expiredSub = UserSubscription.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(USER_ID).build())
                .plan(paidHealthPlan(10, 50, 30))
                .status(SubscriptionStatus.ACTIVE)
                .endDate(OffsetDateTime.now().minusDays(1))
                .build();
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(
                eq(USER_ID), eq(SubscriptionStatus.ACTIVE),
                argThat(date -> date.isAfter(expiredSub.getEndDate()))))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(User.builder().id(USER_ID).role(UserRole.PATIENT).build()));
        when(planRepository.findFirstByVerticalAndIsActiveTrueOrderByPriceAsc(ProductVertical.SALUD_PERSONAL))
                .thenReturn(Optional.of(defaultPlan));

        assertEquals(5, quotaPort.getMaxTriagesPerMonth(USER_ID));
    }

    // ============================================================
    // REGRESIÓN: el fallback de resolvePlan() debe filtrar por
    // vertical (rol del usuario), NO devolver el plan más barato de
    // cualquier vertical. Un médico sin suscripción no debe caer en
    // el FREE de EMPRESAS ($0, sin límites de SALUD_PROFESIONAL).
    // ============================================================

    private PricingPlan freePlan(ProductVertical vertical, String code, Integer maxTriages, Integer maxPatients, Integer trialDays) {
        return PricingPlan.builder()
                .id(UUID.randomUUID())
                .code(code)
                .vertical(vertical)
                .name(code)
                .price(BigDecimal.ZERO)
                .features("[]")
                .maxTriagesPerMonth(maxTriages)
                .maxPatients(maxPatients)
                .trialDays(trialDays)
                .isActive(true)
                .build();
    }

    @Test
    void getMaxPatients_medicoSinSuscripcion_caeEnPlanDeSuVerticalNoEnEmpresas() {
        // El médico NO tiene suscripción activa. Antes del fix, el fallback usaba
        // findFirstByIsActiveTrueOrderByPriceAsc() (plan más barato GLOBAL). En la BD
        // real hay planes a $0 en las tres verticales (FREE Empresas, Personal Free
        // SALUD_PERSONAL, Profesional Trial SALUD_PROFESIONAL), por lo que el médico
        // podía resolver el plan de EMPRESAS (maxPatients=null → "ilimitado").
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(
                eq(PHYSICIAN_ID), eq(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(Optional.empty());
        when(userRepository.findById(PHYSICIAN_ID))
                .thenReturn(Optional.of(User.builder().id(PHYSICIAN_ID).role(UserRole.PHYSICIAN).build()));
        when(planRepository.findFirstByVerticalAndIsActiveTrueOrderByPriceAsc(
                com.kinplatform.pricing.ProductVertical.SALUD_PROFESIONAL))
                .thenReturn(Optional.of(freePlan(
                        com.kinplatform.pricing.ProductVertical.SALUD_PROFESIONAL,
                        "TRIAL", null, 100, 30)));

        // Profesional Trial siembra maxPatients=100 (no ilimitado): debe devolver 100.
        assertEquals(100, quotaPort.getMaxPatients(PHYSICIAN_ID));
    }

    @Test
    void getMaxTriagesPerMonth_pacienteSinSuscripcion_caeEnPlanDeSuVerticalNoEnEmpresas() {
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(
                eq(USER_ID), eq(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(User.builder().id(USER_ID).role(UserRole.PATIENT).build()));
        when(planRepository.findFirstByVerticalAndIsActiveTrueOrderByPriceAsc(
                com.kinplatform.pricing.ProductVertical.SALUD_PERSONAL))
                .thenReturn(Optional.of(freePlan(
                        com.kinplatform.pricing.ProductVertical.SALUD_PERSONAL,
                        "FREE", 3, null, null)));

        assertEquals(3, quotaPort.getMaxTriagesPerMonth(USER_ID));
    }

    @Test
    void hasEligibleSubscription_activaOMuestraEnTrial_devuelveTrue() {
        var plan = freePlan(com.kinplatform.pricing.ProductVertical.SALUD_PROFESIONAL, "TRIAL", null, null, 30);
        var trialSub = UserSubscription.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(PHYSICIAN_ID).role(UserRole.PHYSICIAN).build())
                .plan(plan)
                .status(SubscriptionStatus.TRIAL)
                .endDate(OffsetDateTime.now().plusDays(30))
                .build();
        when(subscriptionRepository.findByUserAndPlanVerticalAndStatus(
                eq(PHYSICIAN_ID), eq(com.kinplatform.pricing.ProductVertical.SALUD_PROFESIONAL),
                eq(SubscriptionStatus.ACTIVE)))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.findByUserAndPlanVerticalAndStatus(
                eq(PHYSICIAN_ID), eq(com.kinplatform.pricing.ProductVertical.SALUD_PROFESIONAL),
                eq(SubscriptionStatus.TRIAL)))
                .thenReturn(Optional.of(trialSub));

        assertTrue(quotaPort.hasEligibleSubscription(
                PHYSICIAN_ID, com.kinplatform.pricing.ProductVertical.SALUD_PROFESIONAL,
                SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIAL));
    }

    @Test
    void hasEligibleSubscription_trialExpirado_devuelveFalse() {
        var expiredTrial = UserSubscription.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(PHYSICIAN_ID).role(UserRole.PHYSICIAN).build())
                .plan(freePlan(com.kinplatform.pricing.ProductVertical.SALUD_PROFESIONAL, "TRIAL", null, null, 30))
                .status(SubscriptionStatus.TRIAL)
                .endDate(OffsetDateTime.now().minusDays(1))
                .build();
        when(subscriptionRepository.findByUserAndPlanVerticalAndStatus(
                eq(PHYSICIAN_ID), eq(com.kinplatform.pricing.ProductVertical.SALUD_PROFESIONAL),
                eq(SubscriptionStatus.ACTIVE)))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.findByUserAndPlanVerticalAndStatus(
                eq(PHYSICIAN_ID), eq(com.kinplatform.pricing.ProductVertical.SALUD_PROFESIONAL),
                eq(SubscriptionStatus.TRIAL)))
                .thenReturn(Optional.of(expiredTrial));

        assertFalse(quotaPort.hasEligibleSubscription(
                PHYSICIAN_ID, com.kinplatform.pricing.ProductVertical.SALUD_PROFESIONAL,
                SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIAL));
    }
}