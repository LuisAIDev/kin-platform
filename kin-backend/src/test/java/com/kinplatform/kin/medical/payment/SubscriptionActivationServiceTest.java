package com.kinplatform.kin.medical.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.common.pricing.PricingPlan;
import com.kinplatform.common.pricing.PricingPlanRepository;
import com.kinplatform.common.pricing.ProductVertical;
import com.kinplatform.common.pricing.SubscriptionStatus;
import com.kinplatform.common.pricing.UserSubscription;
import com.kinplatform.common.pricing.UserSubscriptionRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La activación debe aislarse por vertical: un pago de EMPRESAS no puede
 * renovar ni bloquear una suscripción de salud, y viceversa. Estos tests fijan
 * ese contrato, usado tanto por Wompi como por Stripe.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionActivationServiceTest {

    @Mock
    private PricingPlanRepository planRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSubscriptionRepository subscriptionRepository;

    private SubscriptionActivationService service;
    private UUID userId;
    private UUID planId;
    private User user;
    private PricingPlan empresasPlan;

    @BeforeEach
    void setUp() {
        service = new SubscriptionActivationService(planRepository, userRepository, subscriptionRepository);
        userId = UUID.randomUUID();
        planId = UUID.randomUUID();
        user = User.builder().id(userId).email("empresa@kin.com").build();
        empresasPlan = PricingPlan.builder()
                .id(planId)
                .code("STANDARD")
                .name("Empresas Standard")
                .price(new BigDecimal("49.00"))
                .priceCop(new BigDecimal("190000"))
                .features("[]")
                .vertical(ProductVertical.EMPRESAS)
                .isActive(true)
                .build();
    }

    @Test
    void activateNew_empresas_permiteAltaAunqueExistaActivaDeSalud() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(planRepository.findById(planId)).thenReturn(Optional.of(empresasPlan));
        when(subscriptionRepository.findByUserAndPlanVerticalAndStatus(
                        userId, ProductVertical.EMPRESAS, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.activateNew(userId, planId);

        verify(subscriptionRepository).save(argThat(subscription ->
                subscription.getPlan() == empresasPlan
                        && subscription.getUser() == user
                        && subscription.getStatus() == SubscriptionStatus.ACTIVE));
        verify(subscriptionRepository, never()).findByUserIdAndStatus(any(), any());
    }

    @Test
    void activateNew_empresas_fallaSiYaHayActivaEnEmpresas() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(planRepository.findById(planId)).thenReturn(Optional.of(empresasPlan));
        when(subscriptionRepository.findByUserAndPlanVerticalAndStatus(
                        userId, ProductVertical.EMPRESAS, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(UserSubscription.builder().plan(empresasPlan).build()));

        assertThrows(IllegalArgumentException.class, () -> service.activateNew(userId, planId));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void activateOrRenew_empresas_creaNuevaSiNoHayActivaEnEmpresas() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(planRepository.findById(planId)).thenReturn(Optional.of(empresasPlan));
        when(subscriptionRepository.findActiveByUserAndVertical(
                        eq(userId), eq(SubscriptionStatus.ACTIVE), eq(ProductVertical.EMPRESAS), any()))
                .thenReturn(List.of());
        when(subscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.activateOrRenew(userId, planId);

        verify(subscriptionRepository).save(argThat(subscription ->
                subscription.getPlan() == empresasPlan
                        && subscription.getUser() == user
                        && subscription.getStatus() == SubscriptionStatus.ACTIVE));
        verify(subscriptionRepository, never()).findByUserIdAndStatusAndEndDateAfter(any(), any(), any());
    }

    @Test
    void activateOrRenew_empresas_renuevaSoloLaSuscripcionDeEmpresas() {
        var existing = UserSubscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(empresasPlan)
                .status(SubscriptionStatus.ACTIVE)
                .endDate(OffsetDateTime.now().minusDays(1))
                .messagesUsed(7)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(planRepository.findById(planId)).thenReturn(Optional.of(empresasPlan));
        when(subscriptionRepository.findActiveByUserAndVertical(
                        eq(userId), eq(SubscriptionStatus.ACTIVE), eq(ProductVertical.EMPRESAS), any()))
                .thenReturn(List.of(existing));

        service.activateOrRenew(userId, planId);

        assertTrue(existing.getEndDate().isAfter(OffsetDateTime.now()));
        assertEquals(0, existing.getMessagesUsed());
        verify(subscriptionRepository).save(existing);
        verify(userRepository, never()).save(any());
        verify(subscriptionRepository, never()).findByUserIdAndStatusAndEndDateAfter(any(), any(), any());
    }
}
