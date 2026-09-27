package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PhysicalExamRequest(
        @Min(50) @Max(300) Integer bpSystolic,
        @Min(30) @Max(200) Integer bpDiastolic,
        @Min(30) @Max(250) Integer heartRate,
        @Min(5) @Max(80) Integer respiratoryRate,
        @DecimalMin("30.0") @DecimalMax("45.0") Double temperature,
        @Min(50) @Max(100) Integer spo2,
        @DecimalMin("0.1") @DecimalMax("500.0") Double weightKg,
        @DecimalMin("20") @DecimalMax("250") Double heightCm,
        @Min(3) @Max(15) Integer glasgowScore,
        @Min(0) @Max(10) Integer painScale
) {
}