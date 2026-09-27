package com.kinplatform.kin.health.hce.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InformedConsentResponse(
        UUID id,
        UUID encounterId,
        String procedureName,
        String consentType,
        String documentVersion,
        UUID physicianId,
        OffsetDateTime signedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}