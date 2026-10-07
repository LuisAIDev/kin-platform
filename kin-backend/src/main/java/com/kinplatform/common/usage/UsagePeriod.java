package com.kinplatform.common.usage;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Período de consumo de IA / cuota de proyectos. Mensual por defecto,
 * alineado al ciclo de facturación de Stripe (Fase 2). El backend es la
 * única autoridad que computa el período; el frontend no puede alterarlo.
 */
public record UsagePeriod(OffsetDateTime start, OffsetDateTime end) {

    /** Período mensual que contiene {@code now} (inicio al 1.º del mes). */
    public static UsagePeriod current() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime start = now.with(TemporalAdjusters.firstDayOfMonth())
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        OffsetDateTime end = start.plusMonths(1);
        return new UsagePeriod(start, end);
    }

    public boolean contains(OffsetDateTime instant) {
        return instant != null && !instant.isBefore(start) && instant.isBefore(end);
    }

    /** {@code true} si este período es más reciente que {@code other}. */
    public boolean isAfter(UsagePeriod other) {
        return other == null || start.isAfter(other.start);
    }
}


