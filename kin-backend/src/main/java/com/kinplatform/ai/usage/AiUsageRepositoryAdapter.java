package com.kinplatform.ai.usage;

import com.kinplatform.platform.usage.AiUsagePort;
import com.kinplatform.platform.usage.AiUsageRecord;
import com.kinplatform.platform.usage.UsagePeriod;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA del puerto {@link AiUsagePort}. Las operaciones de reserva y
 * reconciliación son SQL nativo atómico (row lock de PostgreSQL), por lo que
 * dos requests concurrentes no pueden superar el presupuesto.
 */
@Repository
@RequiredArgsConstructor
public class AiUsageRepositoryAdapter implements AiUsagePort {

    private final AiUsageJpaRepository jpa;

    @Override
    @Transactional(readOnly = true)
    public Optional<AiUsageRecord> findByUserAndPeriod(UUID userId, UsagePeriod period) {
        return jpa.findByUserIdAndPeriodStart(userId, period.start())
                .map(e -> new AiUsageRecord(
                        e.getId(),
                        e.getUserId(),
                        new UsagePeriod(e.getPeriodStart(), e.getPeriodEnd()),
                        e.getInputTokens(),
                        e.getOutputTokens(),
                        e.getTotalTokens(),
                        e.getEstimatedCostUsd(),
                        e.getReservedCostUsd(),
                        e.getRequestCount(),
                        e.getCreatedAt(),
                        e.getUpdatedAt()));
    }

    @Override
    @Transactional
    public boolean tryReserve(UUID userId, UsagePeriod period, BigDecimal budgetUsd, BigDecimal estimateUsd) {
        jpa.insertPeriodRowIfMissing(userId, period.start(), period.end());
        int rows = jpa.tryReserve(userId, period.start(), budgetUsd, estimateUsd);
        return rows == 1;
    }

    @Override
    @Transactional
    public void recordActual(
            UUID userId,
            UsagePeriod period,
            BigDecimal reservationUsd,
            BigDecimal actualCostUsd,
            long inputTokens,
            long outputTokens) {
        jpa.insertPeriodRowIfMissing(userId, period.start(), period.end());
        jpa.recordActual(
                userId,
                period.start(),
                reservationUsd == null ? BigDecimal.ZERO : reservationUsd,
                actualCostUsd == null ? BigDecimal.ZERO : actualCostUsd,
                inputTokens,
                outputTokens);
    }
}

