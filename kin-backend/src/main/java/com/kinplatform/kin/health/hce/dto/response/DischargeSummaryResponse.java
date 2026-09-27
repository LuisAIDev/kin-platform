package com.kinplatform.kin.health.hce.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DischargeSummaryResponse(
        UUID id,
        UUID encounterId,
        LocalDate admissionDate,
        LocalDate dischargeDate,
        String dischargeDiagnosisCie10,
        String clinicalSummary,
        String generalRecommendations,
        String dischargeCondition,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}