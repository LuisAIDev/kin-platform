package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PatientIdentificationRequest(
        @NotBlank @Pattern(regexp = "CC|CE|TI|RC|PA|SC|PE|DE|PT") String documentType,
        @NotBlank @Size(max = 20) String documentNumber,
        @Pattern(regexp = "^[A-Z]{1,3}[+-]$") String rhFactor,
        @Pattern(regexp = "CONTRIBUTIVO|SUBSIDIADO|VINCULADO|PARTICULAR") String regimen
) {
}