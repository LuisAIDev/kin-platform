package com.kinplatform.kin.health.followup.adapter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
 * Entidad JPA de evolución del paciente (tabla {@code patient_evolutions},
 * ADR-033). Los signos vitales se almacenan como JSONB (String JSON, patrón
 * {@code patient_profiles}).
 */
@Entity
@Table(name = "patient_evolutions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientEvolutionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "physician_id", nullable = false)
    private UUID physicianId;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(name = "symptoms")
    private String symptoms;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vitals", columnDefinition = "jsonb")
    private String vitals;

    @Column(name = "medication_adherence")
    private Boolean medicationAdherence;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
