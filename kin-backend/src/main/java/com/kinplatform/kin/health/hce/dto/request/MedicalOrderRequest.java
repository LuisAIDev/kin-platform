package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalOrderRequest {

    @NotNull
    @Pattern(regexp = "MEDICATION|PROCEDURE|LAB_EXAM|IMAGING|DIET|NURSING_CARE|OTHER")
    private String orderType;

    @NotNull
    @Pattern(regexp = "STAT|URGENT|ROUTINE|SCHEDULED")
    private String priority;

    @Size(max = 20)
    private String cupsCode;

    @Size(max = 200)
    private String drugName;

    @Size(max = 200)
    private String dose;

    @Size(max = 200)
    private String doseUnit;

    @Size(max = 200)
    private String route;

    @Size(max = 200)
    private String frequency;

    private Integer durationDays;

    @AssertTrue(message = "cupsCode es obligatorio para PROCEDURE, LAB_EXAM e IMAGING")
    private boolean isCupsCodeValid() {
        if (orderType == null) {
            return true;
        }
        if (orderType.equals("PROCEDURE") || orderType.equals("LAB_EXAM") || orderType.equals("IMAGING")) {
            return cupsCode != null && !cupsCode.trim().isEmpty();
        }
        return true;
    }
}