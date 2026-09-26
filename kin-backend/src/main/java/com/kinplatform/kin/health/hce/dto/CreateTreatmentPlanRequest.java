package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.TreatmentPlan.Conduct;
import com.kinplatform.kin.health.hce.entity.TreatmentPlan.Prognosis;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTreatmentPlanRequest {

    @NotNull(message = "Encounter ID is required")
    private UUID encounterId;

    @NotNull(message = "Conduct is required")
    private Conduct conduct;

    @Size(max = 1000, message = "Therapeutic goals cannot exceed 1000 characters each")
    private String[] therapeuticGoals;

    @Size(max = 5000, message = "Follow-up plan cannot exceed 5000 characters")
    private String followupPlan;

    @Size(max = 2000, message = "Reevaluation criteria cannot exceed 2000 characters")
    private String reevaluationCriteria;

    private Prognosis prognosis;

    private Duration estimatedDuration;
}