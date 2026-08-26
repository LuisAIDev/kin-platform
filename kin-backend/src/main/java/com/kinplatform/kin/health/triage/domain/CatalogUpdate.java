package com.kinplatform.kin.health.triage.domain;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Actualización masiva del catálogo de triaje (ADR-028, fase profesional).
 *
 * <p>Contiene los síntomas, condiciones y relaciones nuevos/actualizados que un
 * {@code KnowledgeSource} médico (p. ej. un dataset CIE-10/WHO verificado) aporta
 * para ampliar el catálogo. El dominio define la estructura; la infraestructura
 * decide cómo persistirla (upsert idempotente).</p>
 */
public record CatalogUpdate(
        String source,
        List<Symptom> symptoms,
        List<Condition> conditions,
        List<SymptomConditionRelation> relations,
        OffsetDateTime appliedAt) {

    public CatalogUpdate {
        source = source == null ? "" : source;
        symptoms = symptoms == null ? List.of() : List.copyOf(symptoms);
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        relations = relations == null ? List.of() : List.copyOf(relations);
        appliedAt = appliedAt == null ? OffsetDateTime.now() : appliedAt;
    }

    public boolean isEmpty() {
        return symptoms.isEmpty() && conditions.isEmpty() && relations.isEmpty();
    }

    public static CatalogUpdate empty() {
        return new CatalogUpdate("", List.of(), List.of(), List.of(), OffsetDateTime.now());
    }

    public static CatalogUpdate of(
            String source,
            List<Symptom> symptoms,
            List<Condition> conditions,
            List<SymptomConditionRelation> relations) {
        return new CatalogUpdate(source, symptoms, conditions, relations, OffsetDateTime.now());
    }
}
