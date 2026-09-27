package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record DiagnosisRequest(
        @NotBlank @Pattern(regexp = "[A-Z]\\d{2}(\\.\\d{1,2})?") String cie10Code,
        @NotNull @Pattern(regexp = "PRINCIPAL|SECUNDARIO|COMORBILIDAD|COMPLICACION|INGRESO|EGRESO") String diagnosisType,
        @NotNull @Pattern(regexp = "CONFIRMED|PRESUMPTIVE|RULED_OUT|WORKING") String certainty,
        String supportedBy,
        LocalDate onsetDate,
        LocalDate resolutionDate
) {
}