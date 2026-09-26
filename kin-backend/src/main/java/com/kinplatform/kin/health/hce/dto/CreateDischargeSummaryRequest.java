package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.DischargeSummary.DischargeCondition;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDischargeSummaryRequest {

    @NotNull(message = "Encounter ID is required")
    private UUID encounterId;

    @NotNull(message = "Admission ID is required")
    private UUID admissionId;

    @NotNull(message = "Admission date is required")
    private Instant admissionDate;

    @NotNull(message = "Discharge date is required")
    private Instant dischargeDate;

    @NotNull(message = "Discharge diagnosis CIE-10 is required")
    @Size(max = 10, message = "CIE-10 code cannot exceed 10 characters")
    private String dischargeDiagnosisCie10;

    @Size(max = 10, message = "CIE-10 code cannot exceed 10 characters")
    private String admissionDiagnosisCie10;

    @Size(max = 1000, message = "Each secondary diagnosis cannot exceed 1000 characters")
    private String[] secondaryDiagnosesCie10;

    @NotNull(message = "Clinical summary is required")
    @Size(max = 10000, message = "Clinical summary cannot exceed 10000 characters")
    private String clinicalSummary;

    private String[] proceduresPerformed;

    private String[] complications;

    private DischargeCondition dischargeCondition;

    @Size(max = 50, message = "Discharge disposition cannot exceed 50 characters")
    private String dischargeDisposition;

    private String dischargeMedications;

    private String followupAppointments;

    private String[] alarmSigns;

    @Size(max = 5000, message = "General recommendations cannot exceed 5000 characters")
    private String generalRecommendations;

    @Size(max = 500, message = "Physician signature hash cannot exceed 500 characters")
    private String physicianSignatureHash;
}