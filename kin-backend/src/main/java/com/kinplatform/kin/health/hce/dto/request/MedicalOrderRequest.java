package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MedicalOrderRequest(
        @NotNull @Pattern(regexp = "MEDICATION|PROCEDURE|LAB_EXAM|IMAGING|DIET|NURSING_CARE|OTHER") String orderType,
        @NotNull @Pattern(regexp = "STAT|URGENT|ROUTINE|SCHEDULED") String priority,
        @Size(max = 20) String cupsCode,
        String drugName,
        String dose,
        String doseUnit,
        String route,
        String frequency,
        Integer durationDays
) {
}