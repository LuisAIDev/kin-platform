package com.kinplatform.pricing.service;

import com.kinplatform.pricing.PricingPlan;
import com.kinplatform.pricing.PricingPlanRepository;
import com.kinplatform.pricing.SubscriptionStatus;
import com.kinplatform.pricing.UserSubscription;
import com.kinplatform.pricing.UserSubscriptionRepository;
import com.kinplatform.project.ProjectRepository;
import com.kinplatform.project.ProjectStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
public class SubscriptionValidatorService {

    private final UserSubscriptionRepository subscriptionRepository;
    private final PricingPlanRepository planRepository;
    private final ProjectRepository projectRepository;
    private final CacheManager cacheManager;

    @Autowired
    public SubscriptionValidatorService(
            UserSubscriptionRepository subscriptionRepository,
            PricingPlanRepository planRepository,
            ProjectRepository projectRepository,
            CacheManager cacheManager) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.projectRepository = projectRepository;
        this.cacheManager = cacheManager;
    }

    @Cacheable(value = "projectLimit", key = "#userId")
    public boolean canCreateProject(UUID userId) {
        PricingPlan plan = getCurrentPlan(userId);

        if (plan.getMaxProjects() == null) {
            log.debug("Usuario {} tiene proyectos ilimitados", userId);
            return true;
        }

        long currentProjects = projectRepository.countByUserIdAndStatusNot(userId, ProjectStatus.ARCHIVED);
        boolean canCreate = currentProjects < plan.getMaxProjects();

        log.debug("Usuario {}: proyectos {}/{} - {}",
            userId, currentProjects, plan.getMaxProjects(),
            canCreate ? "PERMITIDO" : "BLOQUEADO");

        return canCreate;
    }

    @Cacheable(value = "messageLimit", key = "#userId")
    public boolean canSendMessage(UUID userId) {
        UserSubscription subscription = getActiveSubscription(userId);
        PricingPlan plan = subscription != null ?
            subscription.getPlan() : getDefaultPlan();

        if (plan.getMessagesPerMonth() == null) {
            log.debug("Usuario {} tiene mensajes ilimitados", userId);
            return true;
        }

        int messagesUsed = subscription != null ?
            subscription.getMessagesUsed() : 0;
        boolean canSend = messagesUsed < plan.getMessagesPerMonth();

        log.debug("Usuario {}: mensajes {}/{} - {}",
            userId, messagesUsed, plan.getMessagesPerMonth(),
            canSend ? "PERMITIDO" : "BLOQUEADO");

        return canSend;
    }

    @Transactional
    public void incrementMessageCount(UUID userId) {
        UserSubscription subscription = getActiveSubscription(userId);
        if (subscription != null) {
            subscription.setMessagesUsed(subscription.getMessagesUsed() + 1);
            subscriptionRepository.save(subscription);
            log.debug("Incrementado contador de mensajes para usuario {}: {}",
                userId, subscription.getMessagesUsed());

            evictCache("messageLimit", userId);
        }
    }

    public int getRemainingMessages(UUID userId) {
        UserSubscription subscription = getActiveSubscription(userId);
        PricingPlan plan = subscription != null ?
            subscription.getPlan() : getDefaultPlan();

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
            .findByUserIdAndStatusAndEndDateAfter(
                userId,
                SubscriptionStatus.ACTIVE,
                OffsetDateTime.now()
            )
            .orElse(null);
    }

    private PricingPlan getDefaultPlan() {
        return planRepository.findFirstByIsActiveTrueOrderByPriceAsc()
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
