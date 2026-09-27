package com.kinplatform.kin.health.hce.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReferralResponse(
        UUID id,
        UUID encounterId,
        String referralType,
        String priority,
        String reason,
        String referredToService,
        String referredToInstitution,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}