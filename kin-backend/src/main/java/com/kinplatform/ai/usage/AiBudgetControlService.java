package com.kinplatform.ai.usage;

import com.kinplatform.kin.usage.AiBudgetExceededException;
import com.kinplatform.kin.usage.AiReservation;
import com.kinplatform.kin.usage.AiUsagePort;
import com.kinplatform.kin.usage.CostEstimator;
import com.kinplatform.kin.usage.HeuristicCostEstimator;
import com.kinplatform.kin.usage.UsagePeriod;
import com.kinplatform.pricing.PricingPlan;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servicio de control de presupuesto de IA (Fase 1). Es el GATE de costo:
 * reserva atómicamente una estimación ANTES de llamar a DeepSeek y reconcilia
 * con el uso real después. El backend es la única autoridad del presupuesto.
 */
@Slf4j
@Service
public class AiBudgetControlService {

    /** Mensaje amigable cuando se agota el presupuesto incluido en el plan. */
    public static final String BUDGET_EXCEEDED_MESSAGE = "Has alcanzado el límite de uso de IA incluido en tu plan. "
            + "Puedes continuar cuando se renueve tu cuota o actualizar tu plan.";

    public static final String REQUEST_CAP_EXCEEDED_MESSAGE =
            "Esta solicitud supera el límite de consumo de IA por petición. "
                    + "Reduce el alcance de la consulta o actualiza tu plan.";

    private final AiUsagePort usagePort;
    private final CostEstimator costEstimator;
    private final boolean costControlEnabled;
    private final BigDecimal maxRequestEstimateUsd;

    public AiBudgetControlService(
            AiUsagePort usagePort,
            CostEstimator costEstimator,
            @Value("${kin.ai.cost-control.enabled:true}") boolean costControlEnabled,
            @Value("${kin.ai.max-request-estimate-usd:1.00}") BigDecimal maxRequestEstimateUsd) {
        this.usagePort = usagePort;
        this.costEstimator = costEstimator;
        this.costControlEnabled = costControlEnabled;
        this.maxRequestEstimateUsd = maxRequestEstimateUsd == null || maxRequestEstimateUsd.signum() <= 0
                ? new BigDecimal("1.00")
                : maxRequestEstimateUsd;
        boolean pricesOk = false;
        if (costEstimator instanceof HeuristicCostEstimator h) {
            pricesOk = h.inputPricePer1M().signum() > 0 && h.outputPricePer1M().signum() > 0;
        }
        if (costControlEnabled && !pricesOk) {
            throw new IllegalStateException(
                    "Configuración inválida de control de costo de IA: KIN_AI_COST_CONTROL_ENABLED=true "
                            + "pero los precios de DeepSeek no están configurados (deepseek.cost.input-per-1m / "
                            + "deepseek.cost.output-per-1m, o DEEPSEEK_INPUT_COST_PER_1M / "
                            + "DEEPSEEK_OUTPUT_COST_PER_1M). Configura precios > 0 o desactiva el control "
                            + "(KIN_AI_COST_CONTROL_ENABLED=false).");
        }
    }

    public boolean isCostControlEnabled() {
        return costControlEnabled;
    }

    /**
     * Reserva el presupuesto estimado de un turno. Si el control de costo está
     * desactivado o no hay precios configurados devuelve {@code null} (sin
     * gate). Si el presupuesto es insuficiente o la petición excede el tope
     * por solicitud lanza {@link AiBudgetExceededException} (HTTP 403).
     */
    public AiReservation reserve(
            UUID userId, PricingPlan plan, String userMessage, List<com.kinplatform.common.context.Message> history) {
        if (!isCostControlEnabled()) {
            return null;
        }
        BigDecimal estimate = costEstimator.estimate(
                userMessage,
                history == null
                        ? null
                        : history.stream()
                                .map(com.kinplatform.common.context.Message::content)
                                .toList());
        if (estimate.compareTo(maxRequestEstimateUsd) > 0) {
            log.warn(
                    "AI_REQUEST_REJECTED_CAP userId={} estimate=${} maxPerRequest=${}",
                    userId,
                    estimate,
                    maxRequestEstimateUsd);
            throw new AiBudgetExceededException(REQUEST_CAP_EXCEEDED_MESSAGE);
        }
        BigDecimal budget = plan.getAiBudgetUsd() == null ? BigDecimal.ZERO : plan.getAiBudgetUsd();
        UsagePeriod period = UsagePeriod.current();
        boolean reserved = usagePort.tryReserve(userId, period, budget, estimate);
        if (!reserved) {
            log.warn("AI_REQUEST_REJECTED_BUDGET userId={} estimate=${} budget=${}", userId, estimate, budget);
            throw new AiBudgetExceededException(BUDGET_EXCEEDED_MESSAGE);
        }
        log.info("AI_REQUEST_ALLOWED userId={} estimate=${} period={}", userId, estimate, period.start());
        return new AiReservation(userId, period, estimate);
    }

    /**
     * Reserva un monto estimado provisto por el llamador (flujos que no pueden
     * estimar desde un prompt de chat, p. ej. la generación Enterprise). Usa la
     * misma autoridad ({@link AiUsagePort}, reserva atómica, tope por
     * solicitud). Si el control está desactivado devuelve {@code null}.
     */
    public AiReservation reserveEstimate(UUID userId, PricingPlan plan, BigDecimal estimateUsd) {
        if (!isCostControlEnabled()) {
            return null;
        }
        if (estimateUsd == null || estimateUsd.signum() < 0) {
            throw new IllegalArgumentException("estimateUsd no puede ser negativo");
        }
        if (estimateUsd.compareTo(maxRequestEstimateUsd) > 0) {
            log.warn(
                    "AI_REQUEST_REJECTED_CAP userId={} estimate=${} maxPerRequest=${}",
                    userId,
                    estimateUsd,
                    maxRequestEstimateUsd);
            throw new AiBudgetExceededException(REQUEST_CAP_EXCEEDED_MESSAGE);
        }
        BigDecimal budget = plan.getAiBudgetUsd() == null ? BigDecimal.ZERO : plan.getAiBudgetUsd();
        UsagePeriod period = UsagePeriod.current();
        boolean reserved = usagePort.tryReserve(userId, period, budget, estimateUsd);
        if (!reserved) {
            log.warn("AI_REQUEST_REJECTED_BUDGET userId={} estimate=${} budget=${}", userId, estimateUsd, budget);
            throw new AiBudgetExceededException(BUDGET_EXCEEDED_MESSAGE);
        }
        log.info("AI_REQUEST_ALLOWED_ESTIMATE userId={} estimate=${} period={}", userId, estimateUsd, period.start());
        return new AiReservation(userId, period, estimateUsd);
    }

    /** Reconciliación de la reserva con el uso real reportado por el proveedor. */
    public void recordActual(AiReservation reservation, long inputTokens, long outputTokens) {
        if (reservation == null) {
            return;
        }
        BigDecimal actual = costEstimator.costOf(inputTokens, outputTokens);
        usagePort.recordActual(
                reservation.userId(),
                reservation.period(),
                reservation.estimatedCostUsd(),
                actual,
                inputTokens,
                outputTokens);
        log.info(
                "AI_USAGE_RECORDED userId={} input={} output={} actual=${} period={}",
                reservation.userId(),
                inputTokens,
                outputTokens,
                actual,
                reservation.period().start());
    }

    /** Resumen de uso del período vigente para exponer al frontend. */
    public AiUsageSummary summary(UUID userId, PricingPlan plan) {
        UsagePeriod period = UsagePeriod.current();
        var record = usagePort.findByUserAndPeriod(userId, period);
        BigDecimal budget = plan.getAiBudgetUsd() == null ? BigDecimal.ZERO : plan.getAiBudgetUsd();
        BigDecimal used = record.map(r -> r.estimatedCostUsd()).orElse(BigDecimal.ZERO);
        BigDecimal reserved = record.map(r -> r.reservedCostUsd()).orElse(BigDecimal.ZERO);
        BigDecimal remaining = budget.subtract(used).subtract(reserved).max(BigDecimal.ZERO);
        return new AiUsageSummary(
                isCostControlEnabled(),
                used,
                reserved,
                budget,
                remaining,
                record.map(r -> r.inputTokens()).orElse(0L),
                record.map(r -> r.outputTokens()).orElse(0L),
                record.map(r -> r.totalTokens()).orElse(0L),
                record.map(r -> r.requestCount()).orElse(0),
                period.start(),
                period.end());
    }
}

