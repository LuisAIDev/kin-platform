package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.PhysicalExam.PainScaleType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhysicalExamResponse {

    private UUID id;
    private UUID encounterId;
    private UUID evolutionId;
    private UUID patientId;
    private UUID physicianId;
    private Instant recordedAt;
    private Integer bpSystolic;
    private Integer bpDiastolic;
    private Integer heartRate;
    private Integer respiratoryRate;
    private BigDecimal temperature;
    private Integer spo2;
    private BigDecimal weightKg;
    private BigDecimal heightCm;
    private BigDecimal bmi;
    private Integer glasgowScore;
    private Integer painScale;
    private PainScaleType painScaleType;
    private String generalAppearance;
    private String headNeck;
    private String cardiovascular;
    private String respiratory;
    private String abdominal;
    private String neurological;
    private String musculoskeletal;
    private String skin;
    private String genitourinary;
    private String psychiatric;
    private String validatedScales;
    private Instant createdAt;
    private Instant updatedAt;
}