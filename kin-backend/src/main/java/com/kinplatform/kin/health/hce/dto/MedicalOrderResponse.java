package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.MedicalOrder.OrderType;
import com.kinplatform.kin.health.hce.entity.MedicalOrder.Priority;
import com.kinplatform.kin.health.hce.entity.MedicalOrder.Route;
import com.kinplatform.kin.health.hce.entity.MedicalOrder.Status;
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
public class MedicalOrderResponse {

    private UUID id;
    private UUID treatmentPlanId;
    private UUID encounterId;
    private UUID patientId;
    private UUID physicianId;
    private OrderType orderType;
    private String drugName;
    private String dose;
    private String doseUnit;
    private Route route;
    private String frequency;
    private Integer durationDays;
    private String cupsCode;
    private String cupsDescription;
    private String bodySite;
    private Priority priority;
    private Status status;
    private String instructions;
    private Instant orderedAt;
    private Instant executedAt;
    private UUID executedBy;
    private String executionNotes;
    private Instant createdAt;
    private Instant updatedAt;
}
