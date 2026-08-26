package com.kinplatform.kin.health.triage.adapter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA del catálogo de condiciones (tabla {@code conditions}, ADR-028).
 */
@Entity
@Table(name = "conditions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConditionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 160)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "icd_code", length = 20)
    private String icdCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private com.kinplatform.kin.health.triage.domain.Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false, length = 20)
    private com.kinplatform.kin.health.triage.domain.Urgency urgency;

    @Column(name = "recommendation", length = 255)
    private String recommendation;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 20)
    private com.kinplatform.kin.health.triage.domain.ValidationStatus validationStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }
}
