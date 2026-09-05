package com.kinplatform.kin.health.triage.share.adapter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA del enlace temporal de compartición de triaje
 * (tabla {@code triage_share_links}).
 */
@Entity
@Table(name = "triage_share_links")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriageShareLinkEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "triage_id", nullable = false)
    private UUID triageId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "token", nullable = false, length = 36)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (createdBy == null) {
            createdBy = patientId;
        }
    }
}
