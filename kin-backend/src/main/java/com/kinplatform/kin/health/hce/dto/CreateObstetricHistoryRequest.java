package com.kinplatform.kin.health.hce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateObstetricHistoryRequest {

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    @Min(value = 0, message = "Gravida cannot be negative")
    private Integer gravida;

    @Min(value = 0, message = "Para cannot be negative")
    private Integer para;

    @Min(value = 0, message = "Abortions cannot be negative")
    private Integer abortions;

    @Min(value = 0, message = "Ectopic pregnancies cannot be negative")
    private Integer ectopicPregnancies;

    @Min(value = 0, message = "Stillbirths cannot be negative")
    private Integer stillbirths;

    @Min(value = 0, message = "Living children cannot be negative")
    private Integer livingChildren;

    private Boolean currentPregnancy;

    private LocalDate lmp;

    private LocalDate estimatedEdd;

    @Min(value = 0, message = "Gestational weeks cannot be negative")
    private Integer gestationalWeeks;

    @Min(value = 0, message = "Prenatal controls cannot be negative")
    private Integer prenatalControls;

    private String previousDeliveries;

    @Size(max = 30, message = "Breastfeeding status cannot exceed 30 characters")
    private String breastfeedingStatus;

    @Min(value = 0, message = "Breastfeeding duration months cannot be negative")
    private Integer breastfeedingDurationMonths;

    private String obstetricComplications;
}