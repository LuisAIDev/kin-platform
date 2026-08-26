package com.kinplatform.kin.health.triage.api;

import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import com.kinplatform.kin.health.triage.domain.Urgency;
import java.util.List;
import java.util.UUID;

/**
 * Condición candidata del endpoint de triaje (ADR-028).
 */
public record TriageConditionResponse(
        UUID conditionId,
        String condition,
        String description,
        double probability,
        Severity severity,
        Urgency urgency,
        String recommendation,
        List<String> matchedSymptoms) {

    public static TriageConditionResponse from(TriageConditionResult result) {
        return new TriageConditionResponse(
                result.conditionId(),
                result.name(),
                result.description(),
                result.probability(),
                result.severity(),
                result.urgency(),
                result.recommendation(),
                result.matchedSymptoms());
    }
}
