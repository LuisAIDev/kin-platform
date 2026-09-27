package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DischargeSummaryRequest {

    @NotNull
    @PastOrPresent
    private LocalDate admissionDate;

    @NotNull
    private LocalDate dischargeDate;

    @AssertTrue(message = "dischargeDate debe ser posterior o igual a admissionDate")
    private boolean isDischargeDateValid() {
        if (admissionDate == null || dischargeDate == null) {
            return true;
        }
        return !dischargeDate.isBefore(admissionDate);
    }

    @NotBlank
    @Pattern(regexp = "[A-Z]\\d{2}(\\.\\d{1,2})?")
    private String dischargeDiagnosisCie10;

    @NotBlank
    @Size(max = 5000)
    private String clinicalSummary;

    @Size(max = 500)
    private String generalRecommendations;

    @Pattern(regexp = "STABLE|IMPROVED|UNCHANGED|WORSENED|DECEASED|TRANSFERRED")
    private String dischargeCondition;
}