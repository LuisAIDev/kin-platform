package com.kinplatform.kin.health.hce.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SurgicalHistoryResponse(
        UUID id,
        UUID patientId,
        String procedureCupsCode,
        LocalDate surgeryDate,
        String procedureCupsDescription,
        String diagnosisDescription,
        Integer asaClassification,
        String anesthesiaType,
        String institution,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}