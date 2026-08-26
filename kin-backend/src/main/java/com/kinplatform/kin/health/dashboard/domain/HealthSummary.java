package com.kinplatform.kin.health.dashboard.domain;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Resumen de salud del paciente (ADR-030).
 *
 * <p>Estadísticas agregadas calculadas de forma determinista a partir del
 * historial de triajes y diagnósticos diferenciales: total de consultas,
 * condiciones más frecuentes (top 3), fecha del último triaje y cantidad de
 * recordatorios activos.</p>
 */
public record HealthSummary(
        int totalConsultations,
        int totalDifferentials,
        List<FrequentCondition> topConditions,
        OffsetDateTime lastTriageAt,
        int activeReminders) {

    public HealthSummary {
        topConditions = topConditions == null ? List.of() : List.copyOf(topConditions);
    }

    /**
     * Condición frecuente del resumen.
     *
     * @param name       nombre de la condición
     * @param occurrences cantidad de veces que apareció en el historial
     */
    public record FrequentCondition(String name, int occurrences) {}
}
