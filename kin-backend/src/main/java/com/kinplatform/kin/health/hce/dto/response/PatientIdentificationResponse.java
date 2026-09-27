package com.kinplatform.kin.health.hce.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PatientIdentificationResponse(
        UUID id,
        UUID patientId,
        String documentType,
        String documentNumber,
        String rhFactor,
        String regimen,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}