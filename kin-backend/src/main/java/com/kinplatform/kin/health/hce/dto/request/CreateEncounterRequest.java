package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateEncounterRequest(
        @NotNull UUID patientId,
        @NotNull UUID physicianId,
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 500) String chiefComplaint,
        @NotNull @Pattern(regexp = "OUTPATIENT|INPATIENT|EMERGENCY|TELEMEDICINE|HOME_CARE|DAY_SURGERY") String encounterType
) {
}