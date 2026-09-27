package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PatientHistoryRequest(
        @NotNull @Pattern(regexp = "ALLERGY|SURGERY|MEDICATION|VACCINE|FAMILY|TOXICOLOGICAL|GYNECO_OBSTETRIC") String historyType,
        @NotBlank String description
) {
}