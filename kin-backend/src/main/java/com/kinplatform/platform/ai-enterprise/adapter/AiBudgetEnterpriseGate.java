package com.kinplatform.platform.ai_enterprise.adapter;

import com.kinplatform.common.ai.usage.AiBudgetControlService;
import com.kinplatform.common.ai.usage.ReservationContext;
import com.kinplatform.platform.enterprise.application.EnterpriseAiBudgetGate;
import com.kinplatform.common.usage.AiBudgetExceededException;
import com.kinplatform.common.usage.AiReservation;
import com.kinplatform.common.usage.CostEstimator;
import com.kinplatform.common.pricing.PricingPlan;
import com.kinplatform.common.pricing.service.SubscriptionValidatorService;
import com.kinplatform.platform.project.Project;
import com.kinplatform.platform.project.ProjectRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adaptador del gate de presupuesto Enterprise. Reutiliza la MISMA autoridad de
 * Fase 1 ({@link AiBudgetControlService} + {@link ReservationContext}) para que
 * la generación del proyecto empresarial no pueda evadir el presupuesto de IA.
 *
 * <p>La estimación usa el peor caso del estimador (tope de salida) por las dos
 * llamadas narrativas del flujo (ejecutivo + DOFA); el uso real posterior
 * reconcilia la reserva.</p>
 */
@Slf4j
@Component
public class AiBudgetEnterpriseGate implements EnterpriseAiBudgetGate {

    private static final int NARRATIVE_LLM_CALLS = 2;

    private final ProjectRepository projectRepository;
    private final SubscriptionValidatorService subscriptionValidator;
    private final AiBudgetControlService budgetControlService;
    private final CostEstimator costEstimator;
    private final ReservationContext reservationContext;

    public AiBudgetEnterpriseGate(
            ProjectRepository projectRepository,
            SubscriptionValidatorService subscriptionValidator,
            AiBudgetControlService budgetControlService,
            CostEstimator costEstimator,
            ReservationContext reservationContext) {
        this.projectRepository = projectRepository;
        this.subscriptionValidator = subscriptionValidator;
        this.budgetControlService = budgetControlService;
        this.costEstimator = costEstimator;
        this.reservationContext = reservationContext;
    }

    @Override
    public boolean reserve(UUID projectId) {
        if (projectId == null) {
            return false;
        }
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null || project.getUser() == null) {
            return false;
        }
        UUID userId = project.getUser().getId();
        PricingPlan plan = subscriptionValidator.getCurrentPlan(userId);
        // Peor caso: cada llamada narrativa consume hasta el tope de salida
        // (input y output), por las dos llamadas del flujo.
        long worstTokens = costEstimator.maxOutputTokens();
        BigDecimal estimate =
                costEstimator.costOf(worstTokens * NARRATIVE_LLM_CALLS, worstTokens * NARRATIVE_LLM_CALLS);
        try {
            AiReservation reservation = budgetControlService.reserveEstimate(userId, plan, estimate);
            if (reservation == null) {
                return true; // control desactivado: sin gate
            }
            reservationContext.set(reservation);
            return true;
        } catch (AiBudgetExceededException e) {
            log.warn(
                    "ENTERPRISE_AI_BUDGET_REJECTED projectId={} userId={} estimate=${} : {}",
                    projectId,
                    userId,
                    estimate,
                    e.getMessage());
            return false;
        }
    }

    @Override
    public void clear() {
        reservationContext.clear();
    }
}







