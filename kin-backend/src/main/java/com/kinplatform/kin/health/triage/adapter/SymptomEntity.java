package com.kinplatform.kin.health.triage.adapter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad JPA del catálogo de síntomas (tabla {@code symptoms}, ADR-028).
 */
@Entity
@Table(name = "symptoms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SymptomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 120)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "icd_code", length = 20)
    private String icdCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "aliases", nullable = false, columnDefinition = "jsonb")
    private java.util.List<String> aliases;

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
