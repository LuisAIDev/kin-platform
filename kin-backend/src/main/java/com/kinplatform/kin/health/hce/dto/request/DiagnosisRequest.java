package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisRequest {

    @NotBlank
    @Pattern(regexp = "[A-Z]\\d{2}(\\.\\d{1,2})?")
    private String cie10Code;

    @NotNull
    @Pattern(regexp = "PRINCIPAL|SECUNDARIO|COMORBILIDAD|COMPLICACION|INGRESO|EGRESO")
    private String diagnosisType;

    @NotNull
    @Pattern(regexp = "CONFIRMED|PRESUMPTIVE|RULED_OUT|WORKING")
    private String certainty;

    @Size(max = 500)
    private String supportedBy;

    private LocalDate onsetDate;

    private LocalDate resolutionDate;
}