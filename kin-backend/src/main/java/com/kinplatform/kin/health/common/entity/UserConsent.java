package com.kinplatform.kin.health.common.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "user_consents",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_consents_user_type_version", columnNames = {"user_id", "consent_type", "version"}),
        indexes = {
                @Index(name = "idx_user_consents_user_id", columnList = "user_id"),
                @Index(name = "idx_user_consents_type", columnList = "consent_type"),
                @Index(name = "idx_user_consents_accepted", columnList = "accepted"),
                @Index(name = "idx_user_consents_updated_at", columnList = "updated_at")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 50)
    private ConsentType consentType;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "accepted", nullable = false)
    @Builder.Default
    private Boolean accepted = false;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revocation_reason", length = 1000)
    private String revocationReason;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "document_hash", length = 64)
    private String documentHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum ConsentType {
        TERMS_OF_SERVICE,
        PRIVACY_POLICY,
        HEALTH_DATA,
        MARKETING,
        DATA_SHARING
    }
}