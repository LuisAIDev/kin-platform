package com.kinplatform.kin.health.hce.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PatientHistoryResponse(
        UUID id,
        UUID patientId,
        String historyType,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}