package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformedConsentRequest {

    @NotBlank
    @Size(max = 200)
    private String procedureName;

    @NotNull
    @Pattern(regexp = "SURGICAL|INVASIVE|ANESTHESIA|TRANSFUSION|RESEARCH|TELEMEDICINE|OTHER")
    private String consentType;

    @NotBlank
    @Size(max = 50)
    private String documentVersion;

    @NotNull
    private UUID physicianId;
}