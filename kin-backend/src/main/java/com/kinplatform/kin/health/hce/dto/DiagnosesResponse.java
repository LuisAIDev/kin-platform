package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.Diagnoses.Classification;
import com.kinplatform.kin.health.hce.entity.Diagnoses.Certainty;
import com.kinplatform.kin.health.hce.entity.Diagnoses.DiagnosisType;
import com.kinplatform.kin.health.hce.entity.Diagnoses.Status;
import java.time.Instant;
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
public class DiagnosesResponse {

    private UUID id;
    private UUID encounterId;
    private UUID patientId;
    private UUID physicianId;
    private String cie10Code;
    private String cie10Description;
    private DiagnosisType diagnosisType;
    private Certainty certainty;
    private Classification classification;
    private String supportedBy;
    private LocalDate onsetDate;
    private LocalDate resolutionDate;
    private Status status;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}