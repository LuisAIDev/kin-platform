package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateEncounterRequest(
        @Size(max = 500) String chiefComplaint,
        @Pattern(regexp = "OUTPATIENT|INPATIENT|EMERGENCY|TELEMEDICINE|HOME_CARE|DAY_SURGERY") String encounterType,
        @NotNull @Pattern(regexp = "OPEN|CLOSED|CANCELLED") String status
) {
}