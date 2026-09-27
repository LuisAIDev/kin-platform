package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record InformedConsentRequest(
        @NotBlank @Size(max = 200) String procedureName,
        @NotNull @Pattern(regexp = "SURGICAL|INVASIVE|ANESTHESIA|TRANSFUSION|RESEARCH|TELEMEDICINE|OTHER") String consentType,
        @NotBlank @Size(max = 50) String documentVersion,
        @NotNull UUID physicianId
) {
}