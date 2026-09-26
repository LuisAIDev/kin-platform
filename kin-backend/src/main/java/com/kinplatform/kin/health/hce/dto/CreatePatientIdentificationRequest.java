package com.kinplatform.kin.health.hce.dto;

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
public class CreatePatientIdentificationRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Document type is required")
    private String documentType;

    @NotNull(message = "Document number is required")
    @Size(max = 20, message = "Document number cannot exceed 20 characters")
    private String documentNumber;

    private LocalDate documentExpeditionDate;
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
}