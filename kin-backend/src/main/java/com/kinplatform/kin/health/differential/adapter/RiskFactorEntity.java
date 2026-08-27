package com.kinplatform.kin.health.differential.adapter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Entidad JPA de factor de riesgo (tabla {@code risk_factors}, ADR-029).
 */
@Entity
@Table(name = "risk_factors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskFactorEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "condition_id", nullable = false)
    private UUID conditionId;

    @Column(name = "factor", nullable = false, length = 120)
    private String factor;

    @Column(name = "weight", nullable = false, columnDefinition = "NUMERIC(5,2)")
    private Double weight;

    @Column(name = "description", length = 255)
    private String description;

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
