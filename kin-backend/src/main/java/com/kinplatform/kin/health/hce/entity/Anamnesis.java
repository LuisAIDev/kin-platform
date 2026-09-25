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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "anamnesis",
       indexes = {
           @Index(name = "idx_anamnesis_encounter_id", columnList = "encounter_id"),
           @Index(name = "idx_anamnesis_patient_id", columnList = "patient_id"),
           @Index(name = "idx_anamnesis_physician_id", columnList = "physician_id")
       })
public class Anamnesis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "encounter_id", nullable = false)
    private UUID encounterId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "physician_id", nullable = false)
    private UUID physicianId;

    @Column(name = "onset_datetime")
    private Instant onsetDatetime;

    @Column(name = "evolution_description", length = 5000)
    private String evolutionDescription;

    @Column(name = "aggravating_factors", length = 2000)
    private String aggravatingFactors;

    @Column(name = "alleviating_factors", length = 2000)
    private String alleviatingFactors;

    @Column(name = "associated_symptoms", length = 2000)
    private String associatedSymptoms;

    @Column(name = "severity_self_reported")
    private Integer severitySelfReported;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "systems_review", columnDefinition = "jsonb")
    @Builder.Default
    private String systemsReview = "{}";

    @Column(name = "previous_episodes")
    @Builder.Default
    private Integer previousEpisodes = 0;

    @Column(name = "previous_treatments", length = 2000)
    private String previousTreatments;

    @Column(name = "functional_impact", length = 2000)
    private String functionalImpact;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}