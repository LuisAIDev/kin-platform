package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.InformedConsent.ConsentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInformedConsentRequest {

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    @NotNull(message = "Procedure name is required")
    @Size(max = 200, message = "Procedure name cannot exceed 200 characters")
    private String procedureName;

    @Size(max = 20, message = "CUPS code cannot exceed 20 characters")
    private String procedureCupsCode;

    @NotNull(message = "Consent type is required")
    private ConsentType consentType;

    @NotNull(message = "Document version is required")
    @Size(max = 50, message = "Document version cannot exceed 50 characters")
    private String documentVersion;

    @Size(max = 500, message = "Document storage key cannot exceed 500 characters")
    private String documentStorageKey;

    @Size(max = 500, message = "Patient signature hash cannot exceed 500 characters")
    private String patientSignatureHash;

    @Size(max = 200, message = "Witness 1 name cannot exceed 200 characters")
    private String witness1Name;

    @Size(max = 50, message = "Witness 1 document cannot exceed 50 characters")
    private String witness1Document;

    @Size(max = 500, message = "Witness 1 signature hash cannot exceed 500 characters")
    private String witness1SignatureHash;

    @Size(max = 200, message = "Witness 2 name cannot exceed 200 characters")
    private String witness2Name;

    @Size(max = 50, message = "Witness 2 document cannot exceed 50 characters")
    private String witness2Document;

    @Size(max = 500, message = "Witness 2 signature hash cannot exceed 500 characters")
    private String witness2SignatureHash;

    @Size(max = 500, message = "Physician signature hash cannot exceed 500 characters")
    private String physicianSignatureHash;
}