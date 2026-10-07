package com.kinplatform.common.pricing.service;

import com.kinplatform.common.ai.usage.AiBudgetControlService;
import com.kinplatform.common.ai.usage.AiUsageSummary;
import com.kinplatform.platform.usage.ProjectQuotaPort;
import com.kinplatform.common.pricing.PricingPlan;
import com.kinplatform.common.pricing.PricingPlanRepository;
import com.kinplatform.common.pricing.SubscriptionStatus;
import com.kinplatform.common.pricing.UserSubscription;
import com.kinplatform.common.pricing.UserSubscriptionRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SubscriptionValidatorService {

    private final UserSubscriptionRepository subscriptionRepository;
    private final PricingPlanRepository planRepository;
    private final CacheManager cacheManager;
    private final ProjectQuotaPort projectQuotaPort;
    private final AiBudgetControlService budgetControlService;

    @Autowired
    public SubscriptionValidatorService(
            UserSubscriptionRepository subscriptionRepository,
            PricingPlanRepository planRepository,
            CacheManager cacheManager,
            ProjectQuotaPort projectQuotaPort,
            AiBudgetControlService budgetControlService) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.cacheManager = cacheManager;
        this.projectQuotaPort = projectQuotaPort;
        this.budgetControlService = budgetControlService;
    }

    /**
     * ¿Puede el usuario crear/completar más proyectos? La cuota se mide sobre
     * proyectos COMPLETADOS por período (contador persistente, no decrece al
     * eliminar). PREMIUM (maxProjects {@code null}) es ilimitado.
     */
    @Cacheable(value = "projectLimit", key = "#userId")
    public boolean canCreateProject(UUID userId) {
        PricingPlan plan = getCurrentPlan(userId);

        if (plan.getMaxProjects() == null) {
            log.debug("Usuario {} tiene proyectos ilimitados", userId);
            return true;
        }

        boolean canCreate = projectQuotaPort.canComplete(userId, plan.getMaxProjects());
        log.debug(
                "Usuario {}: completados {}/{} - {}",
                userId,
                projectQuotaPort.completedProjects(userId),
                plan.getMaxProjects(),
                canCreate ? "PERMITIDO" : "BLOQUEADO");
        return canCreate;
    }

    /** Proyectos completados por el usuario en el período vigente. */
    public int getCompletedProjectsUsed(UUID userId) {
        projectQuotaPort.rolloverIfNeeded(userId);
        return projectQuotaPort.completedProjects(userId);
    }

    /**
     * Consume atómicamente una unidad de la cuota de proyectos completados.
     * Devuelve {@code true} si el cupo se aplicó (límite respetado); {@code
     * false} si el usuario ya alcanzó el límite del plan. Dos transiciones a
     * COMPLETED concurrentes no pueden superar el límite (UPDATE condicional).
     */
    @Transactional
    public boolean tryCompleteProject(UUID userId) {
        PricingPlan plan = getCurrentPlan(userId);
        boolean ok = projectQuotaPort.tryIncrementCompleted(userId, plan.getMaxProjects());
        if (ok) {
            evictCache("projectLimit", userId);
        }
        log.debug("Usuario {}: consume cupo de proyecto completado -> {}", userId, ok ? "OK" : "LÍMITE");
        return ok;
    }

    /** Resumen de uso de IA del usuario (para el frontend; solo representación). */
    public AiUsageSummary getAiUsage(UUID userId) {
        return budgetControlService.summary(userId, getCurrentPlan(userId));
    }

    @Cacheable(value = "messageLimit", key = "#userId")
    public boolean canSendMessage(UUID userId) {
        UserSubscription subscription = getActiveSubscription(userId);
        PricingPlan plan = subscription != null ? subscription.getPlan() : getDefaultPlan();

        if (plan.getMessagesPerMonth() == null) {
            log.debug("Usuario {} tiene mensajes ilimitados", userId);
            return true;
        }

        int messagesUsed = subscription != null ? subscription.getMessagesUsed() : 0;
        boolean canSend = messagesUsed < plan.getMessagesPerMonth();

        log.debug(
                "Usuario {}: mensajes {}/{} - {}",
                userId,
                messagesUsed,
                plan.getMessagesPerMonth(),
                canSend ? "PERMITIDO" : "BLOQUEADO");

        return canSend;
    }

    @Transactional
    public void incrementMessageCount(UUID userId) {
        UserSubscription subscription = getActiveSubscription(userId);
        if (subscription != null) {
            subscription.setMessagesUsed(subscription.getMessagesUsed() + 1);
            subscriptionRepository.save(subscription);
            log.debug("Incrementado contador de mensajes para usuario {}: {}", userId, subscription.getMessagesUsed());

            evictCache("messageLimit", userId);
        }
    }

    public int getRemainingMessages(UUID userId) {
        UserSubscription subscription = getActiveSubscription(userId);
        PricingPlan plan = subscription != null ? subscription.getPlan() : getDefaultPlan();

        if (plan.getMessagesPerMonth() == null) {
            return Integer.MAX_VALUE;
        }

        int used = subscription != null ? subscription.getMessagesUsed() : 0;
        return Math.max(0, plan.getMessagesPerMonth() - used);
    }

    public String getAvailableAILevel(UUID userId) {
        PricingPlan plan = getCurrentPlan(userId);
        return plan.getAdvancedAI() ? "PRO" : "FLASH";
    }

    public PricingPlan getCurrentPlan(UUID userId) {
        UserSubscription subscription = getActiveSubscription(userId);
        if (subscription != null) {
            return subscription.getPlan();
        }
        return getDefaultPlan();
    }

    public boolean isSubscriptionActive(UUID userId) {
        UserSubscription subscription = getActiveSubscription(userId);
        if (subscription == null) {
            return false;
        }
        return subscription.getStatus() == SubscriptionStatus.ACTIVE
                && subscription.getEndDate() != null
                && subscription.getEndDate().isAfter(OffsetDateTime.now());
    }

    /**
     * Consulta la suscripci�n activa del usuario. Sin {@code @Cacheable}: este
     * m�todo se invoca por self-invocation desde otros m�todos del mismo bean,
     * por lo que un anotaci�n AOP nunca se disparar�a (cach� muerta) y, adem�s,
     * cachear la entidad JPA devolver�a estado obsoleto tras las mutaciones de
     * {@link #incrementMessageCount(UUID)}.
     */
    public UserSubscription getActiveSubscription(UUID userId) {
        return subscriptionRepository
                .findByUserIdAndStatusAndEndDateAfter(userId, SubscriptionStatus.ACTIVE, OffsetDateTime.now())
                .orElse(null);
    }

    private PricingPlan getDefaultPlan() {
        return planRepository
                .findFirstByIsActiveTrueOrderByPriceAsc()
                .orElseThrow(() -> new RuntimeException("No active pricing plan found"));
    }

    public void evictProjectLimitCache(UUID userId) {
        evictCache("projectLimit", userId);
    }

    private void evictCache(String cacheName, UUID userId) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(userId);
            log.debug("Cache {} invalidado para usuario {}", cacheName, userId);
        }
    }
}



