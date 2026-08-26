package com.kinplatform.kin.health.triage.adapter;

import com.kinplatform.kin.health.triage.domain.Condition;
import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.Symptom;
import com.kinplatform.kin.health.triage.domain.SymptomConditionRelation;
import com.kinplatform.kin.health.triage.domain.Urgency;
import java.util.UUID;

/**
 * Mapeo puro entre el dominio de triaje y las entidades JPA (ADR-028).
 * No contiene lógica de negocio ni depende de Spring.
 */
public final class TriageMapper {

    private TriageMapper() {}

    public static Symptom toSymptom(SymptomEntity entity) {
        if (entity == null) {
            return null;
        }
        return Symptom.of(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getIcdCode(),
                entity.getAliases() == null ? java.util.List.of() : entity.getAliases());
    }

    public static Condition toCondition(ConditionEntity entity) {
        if (entity == null) {
            return null;
        }
        return Condition.of(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getIcdCode(),
                entity.getSeverity() == null ? Severity.LEVE : entity.getSeverity(),
                entity.getUrgency() == null ? Urgency.BAJA : entity.getUrgency(),
                entity.getRecommendation(),
                entity.getValidationStatus() == null
                        ? com.kinplatform.kin.health.triage.domain.ValidationStatus.PENDING
                        : entity.getValidationStatus());
    }

    public static SymptomConditionRelation toRelation(SymptomConditionRelationEntity entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        return SymptomConditionRelation.of(
                entity.getId().getSymptomId(),
                entity.getId().getConditionId(),
                entity.getWeight() == null ? 0.0 : entity.getWeight(),
                Boolean.TRUE.equals(entity.getRequired()));
    }

    public static UUID symptomId(SymptomConditionRelationEntity entity) {
        return entity == null || entity.getId() == null ? null : entity.getId().getSymptomId();
    }

    public static UUID conditionId(SymptomConditionRelationEntity entity) {
        return entity == null || entity.getId() == null ? null : entity.getId().getConditionId();
    }
}
