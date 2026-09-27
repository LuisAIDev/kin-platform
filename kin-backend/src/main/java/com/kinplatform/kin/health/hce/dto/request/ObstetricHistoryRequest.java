package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObstetricHistoryRequest {

    @Min(0)
    private Integer gravida;

    @Min(0)
    private Integer para;

    @Min(0)
    private Integer abortions;

    @Min(0)
    private Integer ectopicPregnancies;

    @Min(0)
    private Integer stillbirths;

    @AssertTrue(message = "gravida debe ser mayor o igual a la suma de para + abortions + ectopicPregnancies + stillbirths")
    private boolean isGravidaValid() {
        if (gravida == null) {
            return true;
        }
        int sum = 0;
        if (para != null) sum += para;
        if (abortions != null) sum += abortions;
        if (ectopicPregnancies != null) sum += ectopicPregnancies;
        if (stillbirths != null) sum += stillbirths;
        return gravida >= sum;
    }

    @Pattern(regexp = "EXCLUSIVE|PARTIAL|NONE|NEVER")
    private String breastfeedingStatus;
}