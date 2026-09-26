package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.SurgicalHistory.AnesthesiaType;
import com.kinplatform.kin.health.hce.entity.SurgicalHistory.SurgeryType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class CreateSurgicalHistoryRequest {

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    @NotNull(message = "Surgery date is required")
    private LocalDate surgeryDate;

    @NotNull(message = "Procedure CUPS code is required")
    @Size(max = 20, message = "Procedure CUPS code cannot exceed 20 characters")
    private String procedureCupsCode;

    @Size(max = 500, message = "Procedure CUPS description cannot exceed 500 characters")
    private String procedureCupsDescription;

    @Size(max = 10, message = "Diagnosis CIE-10 cannot exceed 10 characters")
    private String diagnosisCie10;

    @Size(max = 500, message = "Diagnosis description cannot exceed 500 characters")
    private String diagnosisDescription;

    private SurgeryType surgeryType;

    private AnesthesiaType anesthesiaType;

    private UUID anesthesiologistId;

    @Min(value = 1, message = "ASA classification must be between 1 and 6")
    @Min(value = 6, message = "ASA classification must be between 1 and 6")
    private Integer asaClassification;

    @Min(value = 0, message = "Duration minutes cannot be negative")
    private Integer durationMinutes;

    @Min(value = 0, message = "Estimated blood loss cannot be negative")
    private Integer estimatedBloodLossMl;

    private String complications;

    private UUID surgeonId;

    private UUID assistantSurgeonId;

    @Size(max = 200, message = "Institution cannot exceed 200 characters")
    private String institution;

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;
}