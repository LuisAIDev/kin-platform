package com.kinplatform.platform.usage;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Registro de consumo de IA de un usuario en un período (una fila por
 * usuario y período). {@code estimatedCostUsd} acumula el costo real de las
 * solicitudes ya completadas; {@code reservedCostUsd} acumula las reservas
 * pendientes de reconciliación. El presupuesto disponible se computa como
 * {@code budget - estimated - reserved}.
 */
public record AiUsageRecord(
        UUID id,
        UUID userId,
        UsagePeriod period,
        long inputTokens,
        long outputTokens,
        long totalTokens,
        BigDecimal estimatedCostUsd,
        BigDecimal reservedCostUsd,
        int requestCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public BigDecimal consumedPlusReserved() {
        return estimatedCostUsd.add(reservedCostUsd);
    }
}

