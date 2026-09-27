package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record DischargeSummaryRequest(
        @NotNull LocalDate admissionDate,
        @NotNull LocalDate dischargeDate,
        @NotBlank @Pattern(regexp = "[A-Z]\\d{2}(\\.\\d{1,2})?") String dischargeDiagnosisCie10,
        @NotBlank @Size(max = 5000) String clinicalSummary,
        @Size(max = 500) String generalRecommendations,
        @Pattern(regexp = "STABLE|IMPROVED|UNCHANGED|WORSENED|DECEASED|TRANSFERRED") String dischargeCondition
) {
    @AssertTrue(message = "dischargeDate debe ser posterior o igual a admissionDate")
    private boolean isDischargeAfterAdmission() {
        return admissionDate == null || dischargeDate == null
                || !dischargeDate.isBefore(admissionDate);
    }
}