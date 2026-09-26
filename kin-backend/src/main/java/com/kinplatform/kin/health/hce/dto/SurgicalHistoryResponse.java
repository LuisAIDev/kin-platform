package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.SurgicalHistory.AnesthesiaType;
import com.kinplatform.kin.health.hce.entity.SurgicalHistory.SurgeryType;
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
public class SurgicalHistoryResponse {

    private UUID id;
    private UUID patientId;
    private LocalDate surgeryDate;
    private String procedureCupsCode;
    private String procedureCupsDescription;
    private String diagnosisCie10;
    private String diagnosisDescription;
    private SurgeryType surgeryType;
    private AnesthesiaType anesthesiaType;
    private UUID anesthesiologistId;
    private Integer asaClassification;
    private Integer durationMinutes;
    private Integer estimatedBloodLossMl;
    private String complications;
    private UUID surgeonId;
    private UUID assistantSurgeonId;
    private String institution;
    private String notes;
    private UUID recordedBy;
    private Instant recordedAt;
    private Instant createdAt;
    private Instant updatedAt;
}