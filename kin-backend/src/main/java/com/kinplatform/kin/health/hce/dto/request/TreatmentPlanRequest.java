package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TreatmentPlanRequest(
        @NotNull @Pattern(regexp = "OBSERVATION|OUTPATIENT_TREATMENT|REFERRAL|HOSPITALIZATION|SURGERY|PALLIATIVE|REHABILITATION") String conduct,
        @Size(max = 10) List<String> therapeuticGoals,
        @Pattern(regexp = "EXCELLENT|GOOD|FAIR|POOR|GUARDED|UNKNOWN") String prognosis
) {
}