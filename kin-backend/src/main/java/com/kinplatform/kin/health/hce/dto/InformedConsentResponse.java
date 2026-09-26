package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.InformedConsent.ConsentType;
import com.kinplatform.kin.health.hce.entity.InformedConsent.Status;
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
public class InformedConsentResponse {

    private UUID id;
    private UUID patientId;
    private String procedureName;
    private String procedureCupsCode;
    private ConsentType consentType;
    private String documentVersion;
    private String documentStorageKey;
    private String patientSignatureHash;
    private String witness1Name;
    private String witness1Document;
    private String witness1SignatureHash;
    private String witness2Name;
    private String witness2Document;
    private String witness2SignatureHash;
    private UUID physicianId;
    private String physicianSignatureHash;
    private Instant signedAt;
    private Instant expiresAt;
    private Instant revokedAt;
    private String revocationReason;
    private Status status;
    private Instant createdAt;
    private Instant updatedAt;
}