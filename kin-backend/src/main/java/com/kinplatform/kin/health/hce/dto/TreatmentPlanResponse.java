package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.TreatmentPlan.Conduct;
import com.kinplatform.kin.health.hce.entity.TreatmentPlan.Prognosis;
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
public class TreatmentPlanResponse {

    private UUID id;
    private UUID encounterId;
    private UUID patientId;
    private UUID physicianId;
    private Conduct conduct;
    private String[] therapeuticGoals;
    private String followupPlan;
    private String reevaluationCriteria;
    private Prognosis prognosis;
    private Duration estimatedDuration;
    private Instant createdAt;
    private Instant updatedAt;
}