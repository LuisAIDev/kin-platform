package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SurgicalHistoryRequest(
        @NotBlank @Size(max = 20) String procedureCupsCode,
        @NotNull @PastOrPresent LocalDate surgeryDate,
        @Size(max = 500) String procedureCupsDescription,
        @Size(max = 500) String diagnosisDescription,
        @Min(1) @Max(6) Integer asaClassification,
        @Pattern(regexp = "GENERAL|REGIONAL_EPIDURAL|REGIONAL_SPINAL|REGIONAL_PLEXUS|LOCAL|SEDATION|NONE") String anesthesiaType,
        @Size(max = 200) String institution
) {
}