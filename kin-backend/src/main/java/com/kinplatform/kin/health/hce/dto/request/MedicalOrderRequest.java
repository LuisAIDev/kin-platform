package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record MedicalOrderRequest(
        @NotNull @Pattern(regexp = "MEDICATION|PROCEDURE|LAB_EXAM|IMAGING|DIET|NURSING_CARE|OTHER") String orderType,
        @NotNull @Pattern(regexp = "STAT|URGENT|ROUTINE|SCHEDULED") String priority,
        @Size(max = 20) String cupsCode,
        @Size(max = 200) String drugName,
        @Size(max = 200) String dose,
        @Size(max = 200) String doseUnit,
        @Size(max = 200) String route,
        @Size(max = 200) String frequency,
        Integer durationDays
) {
    @AssertTrue(message = "cupsCode es obligatorio para PROCEDURE, LAB_EXAM, IMAGING")
    private boolean isCupsCodeValidForType() {
        if (orderType == null) return true;
        Set<String> requiresCups = Set.of("PROCEDURE", "LAB_EXAM", "IMAGING");
        return !requiresCups.contains(orderType)
                || (cupsCode != null && !cupsCode.isBlank());
    }
}