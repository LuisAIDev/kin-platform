package com.kinplatform.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.pricing.dto.SubscriptionResponse;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PLAN_ID = UUID.randomUUID();

    @Mock
    private UserSubscriptionRepository subscriptionRepository;

    @Mock
    private PricingPlanRepository planRepository;

    @Mock
    private UserRepository userRepository;

    private SubscriptionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionServiceImpl(subscriptionRepository, planRepository, userRepository);
    }

    private User user() {
        return User.builder()
                .id(USER_ID)
                .email("u@kin.com")
                .fullName("User")
                .role(UserRole.FREE)
                .build();
    }

    private PricingPlan plan(BigDecimal price, Integer messagesPerMonth, boolean active) {
        return PricingPlan.builder()
                .id(PLAN_ID)
                .name("Plan")
                .price(price)
                .features("[]")
                .messagesPerMonth(messagesPerMonth)
                .isActive(active)
                .supportLevel(SupportLevel.BASIC)
                .viabilityScoringDetail(ViabilityScoringDetail.BASIC)
                .build();
    }

    private UserSubscription activeSubscription() {
        return UserSubscription.builder()
                .id(UUID.randomUUID())
                .user(user())
                .plan(plan(BigDecimal.ZERO, 10, true))
                .status(SubscriptionStatus.ACTIVE)
                .messagesUsed(5)
                .build();
    }

    @Test
    void subscribe_planGratis_deberiaActivar() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(BigDecimal.ZERO, 10, true)));
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(UserSubscription.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        SubscriptionResponse response = service.subscribe(USER_ID, PLAN_ID);

        assertEquals(PLAN_ID, response.getPlan().getId());
        assertEquals(SubscriptionStatus.ACTIVE, response.getStatus());
        assertEquals(0, response.getMessagesUsed());
        verify(subscriptionRepository).save(any(UserSubscription.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void subscribe_usuarioNoEncontrado_deberiaFallar() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.subscribe(USER_ID, PLAN_ID));
    }

    @Test
    void subscribe_planNoEncontrado_deberiaFallar() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.subscribe(USER_ID, PLAN_ID));
    }

    @Test
    void subscribe_planInactivo_deberiaFallar() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(BigDecimal.ZERO, 10, false)));

        assertThrows(IllegalArgumentException.class, () -> service.subscribe(USER_ID, PLAN_ID));
    }

    @Test
    void subscribe_planDePago_deberiaRechazar() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(new BigDecimal("29.00"), 10, true)));

        assertThrows(IllegalArgumentException.class, () -> service.subscribe(USER_ID, PLAN_ID));
    }

    @Test
    void subscribe_conSuscripcionActiva_deberiaRechazar() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(BigDecimal.ZERO, 10, true)));
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(activeSubscription()));

        assertThrows(IllegalArgumentException.class, () -> service.subscribe(USER_ID, PLAN_ID));
    }

    @Test
    void startTrial_deberiaCrearTrial() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(BigDecimal.ZERO, 10, true)));
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(UserSubscription.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        SubscriptionResponse response = service.startTrial(USER_ID, PLAN_ID);

        assertEquals(SubscriptionStatus.TRIAL, response.getStatus());
        assertTrue(response.getEndDate().isAfter(response.getStartDate()));
    }

    @Test
    void startTrial_planNoEncontrado_deberiaLanzarPlanNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.empty());

        assertThrows(PlanNotFoundException.class, () -> service.startTrial(USER_ID, PLAN_ID));
    }

    @Test
    void startTrial_planInactivo_deberiaFallar() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(BigDecimal.ZERO, 10, false)));

        assertThrows(IllegalArgumentException.class, () -> service.startTrial(USER_ID, PLAN_ID));
    }

    @Test
    void startTrial_conSuscripcionActiva_deberiaRechazar() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(BigDecimal.ZERO, 10, true)));
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(activeSubscription()));

        assertThrows(IllegalArgumentException.class, () -> service.startTrial(USER_ID, PLAN_ID));
    }

    @Test
    void cancelSubscription_deberiaCancelar() {
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(activeSubscription()));
        when(subscriptionRepository.save(any(UserSubscription.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        SubscriptionResponse response = service.cancelSubscription(USER_ID);

        assertEquals(SubscriptionStatus.CANCELLED, response.getStatus());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void cancelSubscription_sinActiva_deberiaFallar() {
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.cancelSubscription(USER_ID));
    }

    @Test
    void getCurrentSubscription_deberiaDevolverLaSuscripcion() {
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Optional.of(activeSubscription()));

        SubscriptionResponse response = service.getCurrentSubscription(USER_ID);

        assertEquals(USER_ID, response.getUserId());
    }

    @Test
    void getCurrentSubscription_sinSuscripcion_deberiaFallar() {
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getCurrentSubscription(USER_ID));
    }

    @Test
    void hasAvailableMessages_sinSuscripcion_deberiaSerFalso() {
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertFalse(service.hasAvailableMessages(USER_ID));
    }

    @Test
    void hasAvailableMessages_mensajesIlimitados_deberiaPermitir() {
        var sub = UserSubscription.builder()
                .plan(plan(BigDecimal.ZERO, null, true))
                .status(SubscriptionStatus.ACTIVE)
                .build();
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(sub));

        assertTrue(service.hasAvailableMessages(USER_ID));
    }

    @Test
    void hasAvailableMessages_dentroDelLimite_deberiaPermitir() {
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(activeSubscription()));

        assertTrue(service.hasAvailableMessages(USER_ID));
    }

    @Test
    void hasAvailableMessages_enElLimite_deberiaBloquear() {
        var sub = UserSubscription.builder()
                .plan(plan(BigDecimal.ZERO, 10, true))
                .status(SubscriptionStatus.ACTIVE)
                .messagesUsed(10)
                .build();
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(sub));

        assertFalse(service.hasAvailableMessages(USER_ID));
    }

    @Test
    void incrementMessagesUsed_deberiaIncrementar() {
        var sub = activeSubscription();
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any(UserSubscription.class))).thenAnswer(i -> i.getArgument(0));

        service.incrementMessagesUsed(USER_ID);

        assertEquals(6, sub.getMessagesUsed());
    }

    @Test
    void incrementMessagesUsed_sinSuscripcion_deberiaFallar() {
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.incrementMessagesUsed(USER_ID));
    }

    @Test
    void incrementMessagesUsed_limiteAlcanzado_deberiaFallar() {
        var sub = UserSubscription.builder()
                .plan(plan(BigDecimal.ZERO, 10, true))
                .status(SubscriptionStatus.ACTIVE)
                .messagesUsed(10)
                .build();
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(sub));

        assertThrows(IllegalStateException.class, () -> service.incrementMessagesUsed(USER_ID));
    }

    @Test
    void resetMonthlyUsage_deberiaReiniciarSoloActivas() {
        var active = activeSubscription();
        var inactive = UserSubscription.builder()
                .id(UUID.randomUUID())
                .status(SubscriptionStatus.CANCELLED)
                .messagesUsed(7)
                .build();
        when(subscriptionRepository.findAll()).thenReturn(List.of(active, inactive));

        service.resetMonthlyUsage();

        assertEquals(0, active.getMessagesUsed());
        assertEquals(7, inactive.getMessagesUsed());
        verify(subscriptionRepository, never()).save(inactive);
    }
}
