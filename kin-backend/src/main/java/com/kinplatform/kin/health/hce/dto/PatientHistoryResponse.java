package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.PatientHistory.HistoryType;
import com.kinplatform.kin.health.hce.entity.PatientHistory.Severity;
import com.kinplatform.kin.health.hce.entity.PatientHistory.Status;
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
public class PatientHistoryResponse {

    private UUID id;
    private UUID patientId;
    private HistoryType historyType;
    private String description;
    private LocalDate onsetDate;
    private LocalDate resolutionDate;
    private Status status;
    private Severity severity;
    private String notes;
    private UUID recordedBy;
    private Instant recordedAt;
    private String details;
    private Instant createdAt;
    private Instant updatedAt;
}
