package com.kinplatform.kin.health.hce.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AnamnesisResponse(
        UUID id,
        UUID encounterId,
        OffsetDateTime onsetDatetime,
        String evolutionDescription,
        Integer severitySelfReported,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}