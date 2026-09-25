package com.kinplatform.kin.health.hce.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "informed_consents",
       indexes = {
           @Index(name = "idx_informed_consents_patient_id", columnList = "patient_id"),
           @Index(name = "idx_informed_consents_type", columnList = "consent_type"),
           @Index(name = "idx_informed_consents_status", columnList = "status"),
           @Index(name = "idx_informed_consents_signed_at", columnList = "signed_at")
       })
public class InformedConsent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "procedure_name", nullable = false, length = 200)
    private String procedureName;

    @Column(name = "procedure_cups_code", length = 20)
    private String procedureCupsCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 30)
    private ConsentType consentType;

    @Column(name = "document_version", nullable = false, length = 50)
    private String documentVersion;

    @Column(name = "document_storage_key", length = 500)
    private String documentStorageKey;

    @Column(name = "patient_signature_hash", length = 500)
    private String patientSignatureHash;

    @Column(name = "witness1_name", length = 200)
    private String witness1Name;

    @Column(name = "witness1_document", length = 50)
    private String witness1Document;

    @Column(name = "witness1_signature_hash", length = 500)
    private String witness1SignatureHash;

    @Column(name = "witness2_name", length = 200)
    private String witness2Name;

    @Column(name = "witness2_document", length = 50)
    private String witness2Document;

    @Column(name = "witness2_signature_hash", length = 500)
    private String witness2SignatureHash;

    @Column(name = "physician_id", nullable = false)
    private UUID physicianId;

    @Column(name = "physician_signature_hash", length = 500)
    private String physicianSignatureHash;

    @Column(name = "signed_at", nullable = false)
    private Instant signedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revocation_reason", length = 500)
    private String revocationReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.VALID;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum ConsentType {
        SURGICAL, INVASIVE, ANESTHESIA, TRANSFUSION, RESEARCH, TELEMEDICINE, OTHER
    }

    public enum Status {
        VALID, EXPIRED, REVOKED, PENDING
    }
}