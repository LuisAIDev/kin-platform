package com.kinplatform.kin.health.dashboard.adapter;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad JPA del perfil del paciente (tabla {@code patient_profiles},
 * ADR-030). El perfil se almacena como JSONB (factores de riesgo y condiciones
 * crónicas); la serialización la gestiona Hibernate con {@code @JdbcTypeCode}.
 */
@Entity
@Table(name = "patient_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientProfileEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profile_data", nullable = false, columnDefinition = "jsonb")
    private String profileData;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = OffsetDateTime.now();
    }
}
