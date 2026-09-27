package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.DischargeSummary.DischargeCondition;
import java.time.Duration;
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
public class DischargeSummaryResponse {

    private UUID id;
    private UUID patientId;
    private UUID admissionId;
    private UUID attendingPhysicianId;
    private Instant admissionDate;
    private Instant dischargeDate;
    private Duration lengthOfStay;
    private String admissionDiagnosisCie10;
    private String dischargeDiagnosisCie10;
    private String[] secondaryDiagnosesCie10;
    private String clinicalSummary;
    private String[] proceduresPerformed;
    private String[] complications;
    private DischargeCondition dischargeCondition;
    private String dischargeDisposition;
    private String dischargeMedications;
    private String followupAppointments;
    private String[] alarmSigns;
    private String generalRecommendations;
    private String physicianSignatureHash;
    private Instant signedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
