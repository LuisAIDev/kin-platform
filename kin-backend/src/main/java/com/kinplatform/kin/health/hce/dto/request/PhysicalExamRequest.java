package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhysicalExamRequest {

    @Min(50)
    @Max(300)
    private Integer bpSystolic;

    @Min(30)
    @Max(200)
    private Integer bpDiastolic;

    @AssertTrue(message = "bpSystolic debe ser mayor que bpDiastolic")
    private boolean isBpSystolicGreaterThanDiastolic() {
        if (bpSystolic == null || bpDiastolic == null) {
            return true;
        }
        return bpSystolic > bpDiastolic;
    }

    @Min(30)
    @Max(250)
    private Integer heartRate;

    @Min(5)
    @Max(80)
    private Integer respiratoryRate;

    @DecimalMin("30.0")
    @DecimalMax("45.0")
    private Double temperature;

    @Min(50)
    @Max(100)
    private Integer spo2;

    @DecimalMin("0.1")
    @DecimalMax("500.0")
    private Double weightKg;

    @DecimalMin("20")
    @DecimalMax("250")
    private Double heightCm;

    @Min(3)
    @Max(15)
    private Integer glasgowScore;

    @Min(0)
    @Max(10)
    private Integer painScale;
}