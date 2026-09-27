package com.kinplatform.kin.health.hce.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClinicalAttachmentResponse(
        UUID id,
        UUID encounterId,
        String attachmentType,
        OffsetDateTime performedAt,
        String storageKey,
        String loincCode,
        String loincDisplay,
        String resultUnit,
        String referenceRangeText,
        String dicomStudyUid,
        String resultValue,
        String resultText,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}