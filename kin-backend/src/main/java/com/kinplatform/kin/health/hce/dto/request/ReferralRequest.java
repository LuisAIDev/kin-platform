package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ReferralRequest {

    @NotNull
    @Pattern(regexp = "INTERCONSULTATION|COUNTERREFERRAL|EMERGENCY|SECOND_OPINION|TRANSFER")
    private String referralType;

    @NotNull
    @Pattern(regexp = "STAT|URGENT|ROUTINE|SCHEDULED")
    private String priority;

    @NotBlank
    @Size(max = 2000)
    private String reason;

    @NotBlank
    @Size(max = 100)
    private String referredToService;

    @Size(max = 200)
    private String referredToInstitution;
}