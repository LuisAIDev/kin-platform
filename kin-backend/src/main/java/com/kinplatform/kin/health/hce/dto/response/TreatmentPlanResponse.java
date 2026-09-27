package com.kinplatform.kin.health.hce.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TreatmentPlanResponse(
        UUID id,
        UUID encounterId,
        String conduct,
        List<String> therapeuticGoals,
        String prognosis,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}