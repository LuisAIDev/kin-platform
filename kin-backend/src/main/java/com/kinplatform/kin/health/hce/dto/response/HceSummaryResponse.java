package com.kinplatform.kin.health.hce.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HceSummaryResponse(
        UUID patientId,
        OffsetDateTime lastEncounterDate,
        Integer activeDiagnosesCount,
        Integer pendingDocumentsCount
) {
}