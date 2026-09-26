package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.PhysicalExam.PainScaleType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePhysicalExamRequest {

    @NotNull(message = "Encounter ID is required")
    private UUID encounterId;

    private UUID evolutionId;

    @Min(value = 50, message = "Systolic BP must be between 50 and 300")
    @Max(value = 300, message = "Systolic BP must be between 50 and 300")
    private Integer bpSystolic;

    @Min(value = 30, message = "Diastolic BP must be between 30 and 200")
    @Max(value = 200, message = "Diastolic BP must be between 30 and 200")
    private Integer bpDiastolic;

    @Min(value = 30, message = "Heart rate must be between 30 and 250")
    @Max(value = 250, message = "Heart rate must be between 30 and 250")
    private Integer heartRate;

    @Min(value = 5, message = "Respiratory rate must be between 5 and 60")
    @Max(value = 60, message = "Respiratory rate must be between 5 and 60")
    private Integer respiratoryRate;

    @DecimalMin(value = "30.0", message = "Temperature must be between 30.0 and 45.0")
    @DecimalMax(value = "45.0", message = "Temperature must be between 30.0 and 45.0")
    private BigDecimal temperature;

    @Min(value = 50, message = "SpO2 must be between 50 and 100")
    @Max(value = 100, message = "SpO2 must be between 50 and 100")
    private Integer spo2;

    @DecimalMin(value = "10.00", message = "Weight must be between 10.00 and 300.00 kg")
    @DecimalMax(value = "300.00", message = "Weight must be between 10.00 and 300.00 kg")
    private BigDecimal weightKg;

    @DecimalMin(value = "30.00", message = "Height must be between 30.00 and 250.00 cm")
    @DecimalMax(value = "250.00", message = "Height must be between 30.00 and 250.00 cm")
    private BigDecimal heightCm;

    @Min(value = 3, message = "Glasgow score must be between 3 and 15")
    @Max(value = 15, message = "Glasgow score must be between 3 and 15")
    private Integer glasgowScore;

    @Min(value = 0, message = "Pain scale must be between 0 and 10")
    @Max(value = 10, message = "Pain scale must be between 0 and 10")
    private Integer painScale;

    private PainScaleType painScaleType;

    @Size(max = 100, message = "General appearance cannot exceed 100 characters")
    private String generalAppearance;

    @Size(max = 2000, message = "Head/neck findings cannot exceed 2000 characters")
    private String headNeck;

    @Size(max = 2000, message = "Cardiovascular findings cannot exceed 2000 characters")
    private String cardiovascular;

    @Size(max = 2000, message = "Respiratory findings cannot exceed 2000 characters")
    private String respiratory;

    @Size(max = 2000, message = "Abdominal findings cannot exceed 2000 characters")
    private String abdominal;

    @Size(max = 2000, message = "Neurological findings cannot exceed 2000 characters")
    private String neurological;

    @Size(max = 2000, message = "Musculoskeletal findings cannot exceed 2000 characters")
    private String musculoskeletal;

    @Size(max = 2000, message = "Skin findings cannot exceed 2000 characters")
    private String skin;

    @Size(max = 2000, message = "Genitourinary findings cannot exceed 2000 characters")
    private String genitourinary;

    @Size(max = 2000, message = "Psychiatric findings cannot exceed 2000 characters")
    private String psychiatric;

    private String validatedScales;
}