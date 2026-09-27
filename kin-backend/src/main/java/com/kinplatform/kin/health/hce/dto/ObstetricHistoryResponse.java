package com.kinplatform.kin.health.hce.dto;

import java.time.Instant;
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
public class ObstetricHistoryResponse {

    private UUID id;
    private UUID patientId;
    private Integer gravida;
    private Integer para;
    private Integer abortions;
    private Integer ectopicPregnancies;
    private Integer stillbirths;
    private Integer livingChildren;
    private Boolean currentPregnancy;
    private LocalDate lmp;
    private LocalDate estimatedEdd;
    private Integer gestationalWeeks;
    private Integer prenatalControls;
    private String previousDeliveries;
    private String breastfeedingStatus;
    private Integer breastfeedingDurationMonths;
    private String obstetricComplications;
    private UUID recordedBy;
    private Instant recordedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
