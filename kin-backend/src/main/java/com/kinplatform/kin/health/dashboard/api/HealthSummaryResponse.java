package com.kinplatform.kin.health.dashboard.api;

import com.kinplatform.kin.health.dashboard.domain.HealthSummary;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Resumen de salud del paciente (ADR-030) para el endpoint REST.
 */
public record HealthSummaryResponse(
        int totalConsultations,
        int totalDifferentials,
        List<FrequentConditionResponse> topConditions,
        OffsetDateTime lastTriageAt,
        int activeReminders) {

    public static HealthSummaryResponse from(HealthSummary summary) {
        return new HealthSummaryResponse(
                summary.totalConsultations(),
                summary.totalDifferentials(),
                summary.topConditions().stream()
                        .map(FrequentConditionResponse::from)
                        .toList(),
                summary.lastTriageAt(),
                summary.activeReminders());
    }

    public record FrequentConditionResponse(String name, int occurrences) {
        static FrequentConditionResponse from(HealthSummary.FrequentCondition fc) {
            return new FrequentConditionResponse(fc.name(), fc.occurrences());
        }
    }
}
