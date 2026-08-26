package com.kinplatform.kin.health.dashboard.api;

import com.kinplatform.kin.health.dashboard.domain.PatientProfile;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Perfil del paciente en el endpoint REST (ADR-030).
 */
public record PatientProfileResponse(
        UUID userId, List<String> riskFactors, List<String> chronicConditions, OffsetDateTime updatedAt) {

    public static PatientProfileResponse from(PatientProfile profile) {
        return new PatientProfileResponse(
                profile.userId(), profile.riskFactors(), profile.chronicConditions(), profile.updatedAt());
    }
}
