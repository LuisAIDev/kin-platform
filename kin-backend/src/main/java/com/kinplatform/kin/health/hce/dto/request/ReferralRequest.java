package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReferralRequest(
        @NotNull @Pattern(regexp = "INTERCONSULTATION|COUNTERREFERRAL|EMERGENCY|SECOND_OPINION|TRANSFER") String referralType,
        @NotNull @Pattern(regexp = "STAT|URGENT|ROUTINE|SCHEDULED") String priority,
        @NotBlank @Size(max = 2000) String reason,
        @NotBlank @Size(max = 100) String referredToService,
        @Size(max = 200) String referredToInstitution
) {
}