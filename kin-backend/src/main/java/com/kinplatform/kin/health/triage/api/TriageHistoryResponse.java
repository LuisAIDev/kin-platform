package com.kinplatform.kin.health.triage.api;

import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entrada del historial de triaje de un paciente (ADR-028).
 */
public record TriageHistoryResponse(
        UUID id, List<String> symptoms, List<TriageConditionResponse> results, OffsetDateTime createdAt) {

    public static TriageHistoryResponse from(TriageConsultation consultation) {
        return new TriageHistoryResponse(
                consultation.id(),
                consultation.symptoms(),
                consultation.results().stream()
                        .map(TriageConditionResponse::from)
                        .toList(),
                consultation.createdAt());
    }
}
