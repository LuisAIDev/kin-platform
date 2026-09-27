package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientIdentificationRequest {

    @NotBlank
    @Pattern(regexp = "CC|CE|TI|RC|PA|SC|PE|DE|PT")
    private String documentType;

    @NotBlank
    @Size(max = 20)
    private String documentNumber;

    @Pattern(regexp = "^[A-Z]{1,3}[+-]$")
    private String rhFactor;

    @Pattern(regexp = "CONTRIBUTIVO|SUBSIDIADO|VINCULADO|PARTICULAR")
    private String regimen;
}