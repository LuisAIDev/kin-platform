package com.kinplatform.kin.health.hce.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DiagnosisResponse(
        UUID id,
        UUID encounterId,
        String cie10Code,
        String diagnosisType,
        String certainty,
        String supportedBy,
        LocalDate onsetDate,
        LocalDate resolutionDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}