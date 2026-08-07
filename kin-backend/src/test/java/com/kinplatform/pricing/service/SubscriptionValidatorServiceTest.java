package com.kinplatform.pricing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.pricing.PricingPlan;
import com.kinplatform.pricing.PricingPlanRepository;
import com.kinplatform.pricing.SubscriptionStatus;
import com.kinplatform.pricing.UserSubscription;
import com.kinplatform.pricing.UserSubscriptionRepository;
import com.kinplatform.project.ProjectRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
class SubscriptionValidatorServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private UserSubscriptionRepository subscriptionRepository;

    @Mock
    private PricingPlanRepository planRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private SubscriptionValidatorService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionValidatorService(
                subscriptionRepository, planRepository, projectRepository, cacheManager);
    }

    private PricingPlan plan(Integer maxProjects, Integer messagesPerMonth, boolean advancedAI) {
        return PricingPlan.builder()
                .id(UUID.randomUUID())
                .name("Plan")
                .price(BigDecimal.ZERO)
                .maxProjects(maxProjects)
                .messagesPerMonth(messagesPerMonth)
                .advancedAI(advancedAI)
                .build();
    }

    private UserSubscription subscription(PricingPlan plan, int messagesUsed) {
        return UserSubscription.builder()
                .id(UUID.randomUUID())
                .plan(plan)
                .messagesUsed(messagesUsed)
                .status(SubscriptionStatus.ACTIVE)
                .endDate(OffsetDateTime.now().plusMonths(1))
                .build();
    }

    private void stubNoSubscription() {
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(planRepository.findFirstByIsActiveTrueOrderByPriceAsc()).thenReturn(Optional.of(plan(3, 20, false)));
    }

    @Test
    void canCreateProject_proyectosIlimitados_deberiaPermitir() {
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(planRepository.findFirstByIsActiveTrueOrderByPriceAsc()).thenReturn(Optional.of(plan(1, null, false)));

        assertTrue(service.canCreateProject(USER_ID));
    }

    @Test
    void canCreateProject_bajoLimite_deberiaPermitir() {
        stubNoSubscription();
        when(projectRepository.countByUserIdAndStatusNot(any(), any())).thenReturn(2L);

        assertTrue(service.canCreateProject(USER_ID));
    }

    @Test
    void canCreateProject_enLimite_deberiaBloquear() {
        stubNoSubscription();
        when(projectRepository.countByUserIdAndStatusNot(any(), any())).thenReturn(3L);

        assertFalse(service.canCreateProject(USER_ID));
    }

    @Test
    void canSendMessage_dentroDelLimite_deberiaPermitir() {
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(any(), any(), any()))
                .thenReturn(Optional.of(subscription(plan(3, 20, false), 10)));

        assertTrue(service.canSendMessage(USER_ID));
    }

    @Test
    void canSendMessage_limiteAlcanzado_deberiaBloquear() {
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(any(), any(), any()))
                .thenReturn(Optional.of(subscription(plan(3, 20, false), 20)));

        assertFalse(service.canSendMessage(USER_ID));
    }

    @Test
    void canSendMessage_sinSuscripcion_yMensajesIlimitados_deberiaPermitir() {
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(planRepository.findFirstByIsActiveTrueOrderByPriceAsc()).thenReturn(Optional.of(plan(3, null, false)));

        assertTrue(service.canSendMessage(USER_ID));
    }

    @Test
    void canSendMessage_sinSuscripcion_yConPlanPorDefecto_deberiaEvaluar() {
        stubNoSubscription();

        assertTrue(service.canSendMessage(USER_ID));
    }

    @Test
    void isSubscriptionActive_deberiaEvaluarEstadoYFecha() {
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(any(), any(), any()))
                .thenReturn(Optional.of(subscription(plan(3, 20, false), 0)));

        assertTrue(service.isSubscriptionActive(USER_ID));
    }

    @Test
    void isSubscriptionActive_sinSuscripcion_deberiaSerFalso() {
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(any(), any(), any()))
                .thenReturn(Optional.empty());

        assertFalse(service.isSubscriptionActive(USER_ID));
    }

    @Test
    void getAvailableAILevel_deberiaDevolverPRO() {
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(planRepository.findFirstByIsActiveTrueOrderByPriceAsc()).thenReturn(Optional.of(plan(3, 20, true)));

        assertEquals("PRO", service.getAvailableAILevel(USER_ID));
    }

    @Test
    void getAvailableAILevel_deberiaDevolverFLASH() {
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(planRepository.findFirstByIsActiveTrueOrderByPriceAsc()).thenReturn(Optional.of(plan(3, 20, false)));

        assertEquals("FLASH", service.getAvailableAILevel(USER_ID));
    }

    @Test
    void incrementMessageCount_deberiaIncrementarYEliminarCache() {
        var sub = subscription(plan(3, 20, false), 5);
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(any(), any(), any()))
                .thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(cacheManager.getCache("messageLimit")).thenReturn(cache);

        service.incrementMessageCount(USER_ID);

        assertEquals(6, sub.getMessagesUsed());
        verify(cache).evict(USER_ID);
    }

    @Test
    void incrementMessageCount_sinSuscripcion_noDeberiaIncrementar() {
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(any(), any(), any()))
                .thenReturn(Optional.empty());

        service.incrementMessageCount(USER_ID);

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void getRemainingMessages_mensajesIlimitados_deberiaDevolverMaxValue() {
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(any(), any(), any()))
                .thenReturn(Optional.of(subscription(plan(3, null, false), 0)));

        assertEquals(Integer.MAX_VALUE, service.getRemainingMessages(USER_ID));
    }

    @Test
    void getRemainingMessages_deberiaCalcularLaDiferencia() {
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(any(), any(), any()))
                .thenReturn(Optional.of(subscription(plan(3, 20, false), 7)));

        assertEquals(13, service.getRemainingMessages(USER_ID));
    }

    @Test
    void evictProjectLimitCache_deberiaInvalidar() {
        when(cacheManager.getCache("projectLimit")).thenReturn(cache);

        service.evictProjectLimitCache(USER_ID);

        verify(cache).evict(USER_ID);
    }

    @Test
    void getCurrentPlan_conSuscripcion_deberiaDevolverElPlan() {
        var sub = subscription(plan(3, 20, true), 0);
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(any(), any(), any()))
                .thenReturn(Optional.of(sub));

        assertEquals(sub.getPlan(), service.getCurrentPlan(USER_ID));
    }
}
