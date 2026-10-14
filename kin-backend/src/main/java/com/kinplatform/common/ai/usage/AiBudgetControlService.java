package com.kinplatform.common.ai.usage;

import com.kinplatform.common.usage.AiBudgetExceededException;
import com.kinplatform.common.usage.AiReservation;
import com.kinplatform.common.usage.AiUsagePort;
import com.kinplatform.common.usage.CostEstimator;
import com.kinplatform.common.usage.HeuristicCostEstimator;
import com.kinplatform.common.usage.UsagePeriod;
import com.kinplatform.common.pricing.PricingPlan;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiBudgetControlService {

    public static final String BUDGET_EXCEEDED_MESSAGE = "Has alcanzado el límite de uso de IA incluido en tu plan. "
            + "Puedes continuar cuando se renueve tu cuota o actualizar tu plan.";

    public static final String REQUEST_CAP_EXCEEDED_MESSAGE =
            "Esta solicitud supera el límite de consumo de IA por petición. "
                    + "Reduce el alcance de la consulta o actualiza tu plan.";

    private final AiUsagePort usagePort;
    private final CostEstimator costEstimator;
    private final UserRepository userRepository;
    private final boolean costControlEnabled;
    private final BigDecimal maxRequestEstimateUsd;

    public AiBudgetControlService(
            AiUsagePort usagePort,
            CostEstimator costEstimator,
            UserRepository userRepository,
            @Value("${kin.ai.cost-control.enabled:true}") boolean costControlEnabled,
            @Value("${kin.ai.max-request-estimate-usd:1.00}") BigDecimal maxRequestEstimateUsd) {
        this.usagePort = usagePort;
        this.costEstimator = costEstimator;
        this.userRepository = userRepository;
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

    private boolean isUnlimited(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getUnlimitedAccess)
                .orElse(false);
    }

    public AiReservation reserve(
            UUID userId, PricingPlan plan, String userMessage, List<com.kinplatform.common.context.Message> history) {
        if (!isCostControlEnabled()) {
            return null;
        }
        // Si el usuario tiene acceso ilimitado, bypass del control de presupuesto
        if (isUnlimited(userId)) {
            return new AiReservation(userId, UsagePeriod.current(), BigDecimal.ZERO);
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

    public AiReservation reserveEstimate(UUID userId, PricingPlan plan, BigDecimal estimateUsd) {
        if (!isCostControlEnabled()) {
            return null;
        }
        // Si el usuario tiene acceso ilimitado, bypass del control de presupuesto
        if (isUnlimited(userId)) {
            return new AiReservation(userId, UsagePeriod.current(), BigDecimal.ZERO);
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

    public AiUsageSummary summary(UUID userId, PricingPlan plan) {
        UsagePeriod period = UsagePeriod.current();
        var record = usagePort.findByUserAndPeriod(userId, period);
        BigDecimal budget = plan.getAiBudgetUsd() == null ? BigDecimal.ZERO : plan.getAiBudgetUsd();
        BigDecimal used = record.map(r -> r.estimatedCostUsd()).orElse(BigDecimal.ZERO);
        BigDecimal reserved = record.map(r -> r.reservedCostUsd()).orElse(BigDecimal.ZERO);
        BigDecimal remaining = budget.subtract(used).subtract(reserved).max(BigDecimal.ZERO);

        // Si el usuario tiene acceso ilimitado, reflejarlo en el summary
        if (isUnlimited(userId)) {
            return new AiUsageSummary(
                    true,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0L, 0L, 0L, 0,
                    UsagePeriod.current().start(),
                    UsagePeriod.current().end()
            );
        }
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
                UsagePeriod.current().start(),
                UsagePeriod.current().end());
    }
}