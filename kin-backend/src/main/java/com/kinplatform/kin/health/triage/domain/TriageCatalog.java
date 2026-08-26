package com.kinplatform.kin.health.triage.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Catálogo inmutable de conocimiento de triaje (ADR-028).
 *
 * <p>Snapshot del catálogo completo (síntomas, condiciones y relaciones) que el
 * {@code TriageEngine} consume de forma determinista. Se carga desde el puerto
 * {@code TriageKnowledgeRepository}; los índices por id/nombre permiten
 * resolución O(1) sin tocar la infraestructura.</p>
 */
public record TriageCatalog(
        List<Symptom> symptoms,
        List<Condition> conditions,
        List<SymptomConditionRelation> relations,
        Map<UUID, Symptom> symptomsById,
        Map<UUID, Condition> conditionsById,
        Map<UUID, List<SymptomConditionRelation>> relationsByCondition) {

    public TriageCatalog {
        symptoms = symptoms == null ? List.of() : List.copyOf(symptoms);
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        relations = relations == null ? List.of() : List.copyOf(relations);
        symptomsById = buildSymptomsById(symptoms);
        conditionsById = buildConditionsById(conditions);
        relationsByCondition = buildRelationsByCondition(relations);
    }

    /**
     * Constructor de conveniencia: construye los índices a partir de las listas.
     */
    public TriageCatalog(List<Symptom> symptoms, List<Condition> conditions, List<SymptomConditionRelation> relations) {
        this(symptoms, conditions, relations, Map.of(), Map.of(), Map.of());
    }

    public static TriageCatalog of(
            List<Symptom> symptoms, List<Condition> conditions, List<SymptomConditionRelation> relations) {
        return new TriageCatalog(symptoms, conditions, relations);
    }

    public static TriageCatalog empty() {
        return new TriageCatalog(List.of(), List.of(), List.of());
    }

    public boolean isEmpty() {
        return symptoms.isEmpty() && conditions.isEmpty();
    }

    public Optional<Symptom> symptomById(UUID id) {
        return Optional.ofNullable(symptomsById.get(id));
    }

    public Optional<Symptom> symptomByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String normalized = name.strip().toLowerCase();
        return symptoms.stream()
                .filter(s -> s.name().equalsIgnoreCase(normalized))
                .findFirst();
    }

    public Optional<Condition> conditionById(UUID id) {
        return Optional.ofNullable(conditionsById.get(id));
    }

    public List<SymptomConditionRelation> relationsForCondition(UUID conditionId) {
        return relationsByCondition.getOrDefault(conditionId, List.of());
    }

    private static Map<UUID, Symptom> buildSymptomsById(List<Symptom> symptoms) {
        return symptoms.stream().collect(Collectors.toUnmodifiableMap(Symptom::id, s -> s));
    }

    private static Map<UUID, Condition> buildConditionsById(List<Condition> conditions) {
        return conditions.stream().collect(Collectors.toUnmodifiableMap(Condition::id, c -> c));
    }

    private static Map<UUID, List<SymptomConditionRelation>> buildRelationsByCondition(
            List<SymptomConditionRelation> relations) {
        return relations.stream()
                .collect(Collectors.groupingBy(
                        SymptomConditionRelation::conditionId,
                        Collectors.collectingAndThen(Collectors.toList(), List::copyOf)));
    }
}
