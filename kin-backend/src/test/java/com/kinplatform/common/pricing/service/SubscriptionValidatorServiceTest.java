package com.kinplatform.common.pricing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.common.ai.usage.AiBudgetControlService;
import com.kinplatform.common.usage.ProjectQuotaPort;
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
    private UserRepository userRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private ProjectQuotaPort projectQuotaPort;

    @Mock
    private AiBudgetControlService budgetControlService;

    private SubscriptionValidatorService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionValidatorService(
                subscriptionRepository,
                planRepository,
                userRepository,
                cacheManager,
                projectQuotaPort,
                budgetControlService);
    }

    private User user(String platform) {
        return User.builder().id(USER_ID).platform(platform).build();
    }

    private PricingPlan plan(Integer maxProjects, Integer messagesPerMonth, boolean advancedAI) {
        return PricingPlan.builder()
                .id(UUID.randomUUID())
                .code("FREE")
                .vertical(ProductVertical.EMPRESAS)
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

    private void stubActive(UserSubscription subscription) {
        when(subscriptionRepository.findActiveByUserAndVertical(
                        eq(USER_ID), eq(SubscriptionStatus.ACTIVE), eq(ProductVertical.EMPRESAS), any()))
                .thenReturn(List.of(subscription));
    }

    private void stubNoSubscription(PricingPlan defaultPlan) {
        when(subscriptionRepository.findActiveByUserAndVertical(
                        eq(USER_ID), eq(SubscriptionStatus.ACTIVE), eq(ProductVertical.EMPRESAS), any()))
                .thenReturn(List.of());
        when(planRepository.findFirstByVerticalAndIsActiveTrueOrderByPriceAsc(ProductVertical.EMPRESAS))
                .thenReturn(Optional.of(defaultPlan));
    }

    @Test
    void canCreateProject_proyectosIlimitados_deberiaPermitir() {
        stubNoSubscription(plan(null, null, false));

        assertTrue(service.canCreateProject(USER_ID));
    }

    @Test
    void canCreateProject_bajoLimite_deberiaPermitir() {
        stubNoSubscription(plan(3, 20, false));
        when(projectQuotaPort.canComplete(USER_ID, 3)).thenReturn(true);

        assertTrue(service.canCreateProject(USER_ID));
    }

    @Test
    void canCreateProject_enLimite_deberiaBloquear() {
        stubNoSubscription(plan(3, 20, false));
        when(projectQuotaPort.canComplete(USER_ID, 3)).thenReturn(false);

        assertFalse(service.canCreateProject(USER_ID));
    }

    @Test
    void canSendMessage_dentroDelLimite_deberiaPermitir() {
        stubActive(subscription(plan(3, 20, false), 10));

        assertTrue(service.canSendMessage(USER_ID));
    }

    @Test
    void canSendMessage_limiteAlcanzado_deberiaBloquear() {
        stubActive(subscription(plan(3, 20, false), 20));

        assertFalse(service.canSendMessage(USER_ID));
    }

    @Test
    void canSendMessage_sinSuscripcion_yMensajesIlimitados_deberiaPermitir() {
        stubNoSubscription(plan(3, null, false));

        assertTrue(service.canSendMessage(USER_ID));
    }

    @Test
    void canSendMessage_sinSuscripcion_yConPlanPorDefecto_deberiaEvaluar() {
        stubNoSubscription(plan(3, 20, false));

        assertTrue(service.canSendMessage(USER_ID));
    }

    @Test
    void isSubscriptionActive_deberiaEvaluarEstadoYFecha() {
        stubActive(subscription(plan(3, 20, false), 0));

        assertTrue(service.isSubscriptionActive(USER_ID));
    }

    @Test
    void isSubscriptionActive_sinSuscripcion_deberiaSerFalso() {
        when(subscriptionRepository.findActiveByUserAndVertical(
                        eq(USER_ID), eq(SubscriptionStatus.ACTIVE), eq(ProductVertical.EMPRESAS), any()))
                .thenReturn(List.of());

        assertFalse(service.isSubscriptionActive(USER_ID));
    }

    @Test
    void isSubscriptionActive_planGratuitoSinFechaFin_deberiaSerActiva() {
        var freeSubscription = subscription(plan(3, 20, false), 0);
        freeSubscription.setEndDate(null);
        stubActive(freeSubscription);

        assertTrue(service.isSubscriptionActive(USER_ID));
    }

    @Test
    void getAvailableAILevel_deberiaDevolverPRO() {
        stubNoSubscription(plan(3, 20, true));

        assertEquals("PRO", service.getAvailableAILevel(USER_ID));
    }

    @Test
    void getAvailableAILevel_deberiaDevolverFLASH() {
        stubNoSubscription(plan(3, 20, false));

        assertEquals("FLASH", service.getAvailableAILevel(USER_ID));
    }

    @Test
    void incrementMessageCount_deberiaIncrementarYEliminarCache() {
        var sub = subscription(plan(3, 20, false), 5);
        stubActive(sub);
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(cacheManager.getCache("messageLimit")).thenReturn(cache);

        service.incrementMessageCount(USER_ID);

        assertEquals(6, sub.getMessagesUsed());
        verify(cache).evict(USER_ID);
    }

    @Test
    void incrementMessageCount_sinSuscripcion_noDeberiaIncrementar() {
        when(subscriptionRepository.findActiveByUserAndVertical(
                        eq(USER_ID), eq(SubscriptionStatus.ACTIVE), eq(ProductVertical.EMPRESAS), any()))
                .thenReturn(List.of());

        service.incrementMessageCount(USER_ID);

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void getRemainingMessages_mensajesIlimitados_deberiaDevolverMaxValue() {
        stubActive(subscription(plan(3, null, false), 0));

        assertEquals(Integer.MAX_VALUE, service.getRemainingMessages(USER_ID));
    }

    @Test
    void getRemainingMessages_deberiaCalcularLaDiferencia() {
        stubActive(subscription(plan(3, 20, false), 7));

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
        stubActive(sub);

        assertEquals(sub.getPlan(), service.getCurrentPlan(USER_ID));
    }

    @Test
    void getCurrentPlan_sinSuscripcion_deberiaUsarElDefaultDeLaVertical() {
        var defaultPlan = plan(3, 20, false);
        when(subscriptionRepository.findActiveByUserAndVertical(
                        eq(USER_ID), eq(SubscriptionStatus.ACTIVE), eq(ProductVertical.EMPRESAS), any()))
                .thenReturn(List.of());
        when(planRepository.findFirstByVerticalAndIsActiveTrueOrderByPriceAsc(ProductVertical.EMPRESAS))
                .thenReturn(Optional.of(defaultPlan));

        assertEquals(defaultPlan, service.getCurrentPlan(USER_ID));
    }

    @Test
    void getCurrentPlan_paciente_deberiaConsultarSuPropiaVertical() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user("SALUD_PERSONAL")));
        var healthPlan = plan(3, 20, false);
        healthPlan.setVertical(ProductVertical.SALUD_PERSONAL);
        var healthSubscription = subscription(healthPlan, 0);
        when(subscriptionRepository.findActiveByUserAndVertical(
                        eq(USER_ID),
                        eq(SubscriptionStatus.ACTIVE),
                        eq(ProductVertical.SALUD_PERSONAL),
                        any()))
                .thenReturn(List.of(healthSubscription));

        assertEquals(healthPlan, service.getCurrentPlan(USER_ID));
        verify(subscriptionRepository, never())
                .findActiveByUserAndVertical(
                        eq(USER_ID), eq(SubscriptionStatus.ACTIVE), eq(ProductVertical.EMPRESAS), any());
    }
}
