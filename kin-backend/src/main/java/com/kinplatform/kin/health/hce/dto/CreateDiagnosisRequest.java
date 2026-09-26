package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.Diagnoses.Classification;
import com.kinplatform.kin.health.hce.entity.Diagnoses.Certainty;
import com.kinplatform.kin.health.hce.entity.Diagnoses.DiagnosisType;
import com.kinplatform.kin.health.hce.entity.Diagnoses.Status;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDiagnosisRequest {

    @NotNull(message = "Encounter ID is required")
    private UUID encounterId;

    @NotNull(message = "CIE-10 code is required")
    @Pattern(regexp = "[A-Z]\\d{2}(\\.\\d{1,2})?", message = "Invalid CIE-10 code format (e.g., A01, K59.1)")
    private String cie10Code;

    @Size(max = 500, message = "CIE-10 description cannot exceed 500 characters")
    private String cie10Description;

    @NotNull(message = "Diagnosis type is required")
    private DiagnosisType diagnosisType;

    private Certainty certainty;

    private Classification classification;

    @Size(max = 500, message = "Supported by cannot exceed 500 characters")
    private String supportedBy;

    private LocalDate onsetDate;

    private LocalDate resolutionDate;

    private Status status;

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;
}