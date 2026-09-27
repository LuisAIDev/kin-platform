package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record ObstetricHistoryRequest(
        @Min(0) Integer gravida,
        @Min(0) Integer para,
        @Min(0) Integer abortions,
        @Min(0) Integer ectopicPregnancies,
        @Min(0) Integer stillbirths,
        @Pattern(regexp = "EXCLUSIVE|PARTIAL|NONE|NEVER") String breastfeedingStatus
) {
    @AssertTrue(message = "gravida debe ser >= suma de para + abortions + ectopicPregnancies + stillbirths")
    private boolean isGravidaConsistent() {
        if (gravida == null) return true;
        int sum = 0;
        if (para != null) sum += para;
        if (abortions != null) sum += abortions;
        if (ectopicPregnancies != null) sum += ectopicPregnancies;
        if (stillbirths != null) sum += stillbirths;
        return gravida >= sum;
    }
}