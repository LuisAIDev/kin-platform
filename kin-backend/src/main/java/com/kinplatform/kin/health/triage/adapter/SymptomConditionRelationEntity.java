package com.kinplatform.kin.health.triage.adapter;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA de relación síntoma ↔ condición (tabla
 * {@code symptom_condition_relations}, ADR-028). Clave compuesta
 * (symptomId, conditionId) y peso entre 0 y 1.
 */
@Entity
@Table(name = "symptom_condition_relations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SymptomConditionRelationEntity {

    @EmbeddedId
    private SymptomConditionRelationId id;

    @Column(name = "weight", nullable = false, columnDefinition = "NUMERIC(5,2)")
    private Double weight;

    @Column(name = "required", nullable = false)
    private Boolean required;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SymptomConditionRelationId implements Serializable {

        @Column(name = "symptom_id")
        private UUID symptomId;

        @Column(name = "condition_id")
        private UUID conditionId;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SymptomConditionRelationId that)) {
                return false;
            }
            return Objects.equals(symptomId, that.symptomId) && Objects.equals(conditionId, that.conditionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(symptomId, conditionId);
        }
    }
}
