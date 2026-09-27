package com.kinplatform.kin.health.hce.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MedicalOrderResponse(
        UUID id,
        UUID encounterId,
        String orderType,
        String priority,
        String cupsCode,
        String drugName,
        String dose,
        String doseUnit,
        String route,
        String frequency,
        Integer durationDays,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}