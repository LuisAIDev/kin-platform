package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
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
public class SurgicalHistoryRequest {

    @NotBlank
    @Size(max = 20)
    private String procedureCupsCode;

    @NotNull
    @PastOrPresent
    private LocalDate surgeryDate;

    @Size(max = 500)
    private String procedureCupsDescription;

    @Size(max = 500)
    private String diagnosisDescription;

    @Min(1)
    @Max(6)
    private Integer asaClassification;

    @Pattern(regexp = "GENERAL|REGIONAL_EPIDURAL|REGIONAL_SPINAL|REGIONAL_PLEXUS|LOCAL|SEDATION|NONE")
    private String anesthesiaType;

    @Size(max = 200)
    private String institution;
}