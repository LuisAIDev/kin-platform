package com.kinplatform.kin.health.hce.dto;

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
public class PatientIdentificationResponse {

    private UUID id;
    private UUID userId;
    private String documentType;
    private String documentNumber;
    private String documentExpeditionDate;
    private String documentExpeditionPlace;
    private String rhFactor;
    private String epsCode;
    private String epsName;
    private String regimen;
    private String guardianName;
    private String guardianDocumentType;
    private String guardianDocumentNumber;
    private String guardianPhone;
    private String guardianRelationship;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;
    private String address;
    private String cityCode;
    private String departmentCode;
    private String zone;
    private Integer stratum;
    private String emailInstitutional;
    private String phoneSecondary;
    private String ethnicity;
    private Boolean displacementVictim;
    private String disabilityCertificate;
    private Instant createdAt;
    private Instant updatedAt;
}
