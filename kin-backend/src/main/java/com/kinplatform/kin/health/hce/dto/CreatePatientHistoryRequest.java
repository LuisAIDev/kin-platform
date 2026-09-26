package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.PatientHistory.HistoryType;
import com.kinplatform.kin.health.hce.entity.PatientHistory.Severity;
import com.kinplatform.kin.health.hce.entity.PatientHistory.Status;
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
public class CreatePatientHistoryRequest {

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    @NotNull(message = "History type is required")
    private HistoryType historyType;

    @NotNull(message = "Description is required")
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    private LocalDate onsetDate;

    private LocalDate resolutionDate;

    private Status status;

    private Severity severity;

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;

    private UUID recordedBy;

    private String details;
}