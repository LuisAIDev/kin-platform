package com.kinplatform.kin.health.triage.adapter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
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
 * Entidad JPA del historial de triaje (tabla {@code triage_consultations},
 * ADR-028). Los síntomas y resultados se serializan como JSONB.
 */
@Entity
@Table(name = "triage_consultations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriageConsultationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "symptoms", nullable = false, columnDefinition = "jsonb")
    private String symptoms;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "results", nullable = false, columnDefinition = "jsonb")
    private String results;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
