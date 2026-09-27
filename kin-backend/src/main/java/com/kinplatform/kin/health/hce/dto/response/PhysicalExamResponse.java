package com.kinplatform.kin.health.hce.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PhysicalExamResponse(
        UUID id,
        UUID encounterId,
        Integer bpSystolic,
        Integer bpDiastolic,
        Integer heartRate,
        Integer respiratoryRate,
        Double temperature,
        Integer spo2,
        Double weightKg,
        Double heightCm,
        Double bmi,
        Integer glasgowScore,
        Integer painScale,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}