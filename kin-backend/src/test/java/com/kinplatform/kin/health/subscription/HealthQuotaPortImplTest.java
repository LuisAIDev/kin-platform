package com.kinplatform.kin.health.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.kinplatform.pricing.PricingPlan;
import com.kinplatform.pricing.PricingPlanRepository;
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
        when(planRepository.findFirstByIsActiveTrueOrderByPriceAsc())
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
        when(planRepository.findFirstByIsActiveTrueOrderByPriceAsc())
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
        when(planRepository.findFirstByIsActiveTrueOrderByPriceAsc())
                .thenReturn(Optional.of(defaultPlan));

        assertEquals(5, quotaPort.getMaxTriagesPerMonth(USER_ID));
    }
}