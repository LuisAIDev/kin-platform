package com.kinplatform.common.ai.usage;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Resumen de uso de IA del usuario en el período vigente, para exponerlo al
 * frontend (solo representación; el backend es la autoridad).
 */
public record AiUsageSummary(
        boolean costControlEnabled,
        BigDecimal budgetUsed,
        BigDecimal budgetReserved,
        BigDecimal budgetLimit,
        BigDecimal budgetRemaining,
        long inputTokens,
        long outputTokens,
        long totalTokens,
        int requestCount,
        OffsetDateTime periodStart,
        OffsetDateTime periodEnd) {}

