package com.kinplatform.kin.health.hce.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ObstetricHistoryResponse(
        UUID id,
        UUID patientId,
        Integer gravida,
        Integer para,
        Integer abortions,
        Integer ectopicPregnancies,
        Integer stillbirths,
        String breastfeedingStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}