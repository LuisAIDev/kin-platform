package com.kinplatform.common.usage;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia del consumo de IA por usuario y período. La reserva
 * ({@link #tryReserve}) debe ser ATÓMICA en PostgreSQL para impedir que dos
 * requests concurrentes superen el presupuesto.
 */
public interface AiUsagePort {

    /** Registro de consumo del usuario en el período actual, si existe. */
    Optional<AiUsageRecord> findByUserAndPeriod(UUID userId, UsagePeriod period);

    /**
     * Reserva atómicamente {@code estimateUsd} dentro del presupuesto
     * {@code budgetUsd}. Devuelve {@code true} si la reserva se aplicó
     * (exactamente una fila actualizada); {@code false} si el presupuesto
     * restante es insuficiente. La condición es:
     * {@code estimated_cost_usd + reserved_cost_usd + estimate <= budget}.
     */
    boolean tryReserve(UUID userId, UsagePeriod period, BigDecimal budgetUsd, BigDecimal estimateUsd);

    /**
     * Reconcatila la reserva con el uso real: consume {@code actualCostUsd} y
     * libera la reserva {@code reservationUsd} asociada.
     */
    void recordActual(
            UUID userId,
            UsagePeriod period,
            BigDecimal reservationUsd,
            BigDecimal actualCostUsd,
            long inputTokens,
            long outputTokens);
}


