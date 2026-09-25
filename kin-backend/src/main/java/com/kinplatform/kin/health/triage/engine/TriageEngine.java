package com.kinplatform.kin.health.triage.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.health.triage.domain.Condition;
import com.kinplatform.kin.health.triage.domain.SymptomConditionRelation;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import com.kinplatform.kin.health.triage.domain.TriageInput;
import com.kinplatform.kin.health.triage.domain.TriageResult;
import com.kinplatform.kin.health.triage.port.TriageKnowledgeRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Motor determinista de triaje (ADR-028).
 *
 * <p>Dado un conjunto de síntomas, calcula para cada condición la suma de los
 * pesos de los síntomas presentes y normaliza el score a una probabilidad en
 * [0,1]. Reglas deterministas sin IA ni LLM:</p>
 *
 * <ol>
 *   <li>Una condición solo es candidata si están presentes todos sus síntomas
 *       {@code required} (síntomas obligatorios).</li>
 *   <li>{@code rawScore(c) = Σ weight(s)} sobre los síntomas presentes.</li>
 *   <li>Se descartan las condiciones sin ningún síntoma coincidente.</li>
 *   <li>La probabilidad se normaliza dividiendo por la suma de los rawScores de
 *       las condiciones candidatas (distribución que suma 1).</li>
 *   <li>Se ordena por probabilidad descendente y se limita el resultado.</li>
 * </ol>
 *
 * <p>Implementa {@link DomainEngine} (fase {@code VALIDATION}, tipo
 * {@code DOMAIN}) para integrarse con la infraestructura común de motores. Usa
 * el puerto {@link TriageKnowledgeRepository} para cargar el catálogo; nunca
 * persiste, no habla con la red y nunca genera prompts.</p>
 */
public class TriageEngine implements DomainEngine<TriageInput, TriageResult> {

    public static final String GENERATOR_NAME = "TriageEngine";
    public static final String ENGINE_VERSION = "v1";

    private final TriageKnowledgeRepository knowledgeRepository;
    private final int maxConditions;

    public TriageEngine(TriageKnowledgeRepository knowledgeRepository) {
        this(knowledgeRepository, 5);
    }

    public TriageEngine(TriageKnowledgeRepository knowledgeRepository, int maxConditions) {
        if (knowledgeRepository == null) {
            throw new IllegalArgumentException("knowledgeRepository no puede ser null");
        }
        this.knowledgeRepository = knowledgeRepository;
        this.maxConditions = Math.max(1, maxConditions);
    }

    @Override
    public EngineMetadata metadata() {
        return EngineMetadata.of(
                GENERATOR_NAME, ENGINE_VERSION, "KIN Architecture Team", EnginePhase.VALIDATION, EngineType.DOMAIN, 55);
    }

    @Override
    public TriageResult evaluate(TriageInput input) {
        if (input == null || input.isEmpty()) {
            return TriageResult.empty();
        }
        TriageCatalog catalog = knowledgeRepository.loadCatalog();
        return evaluate(input, catalog);
    }

    /**
     * Evaluación pura contra un catálogo dado (determinista y testeable).
     */
    public TriageResult evaluate(TriageInput input, TriageCatalog catalog) {
        if (input == null || input.isEmpty() || catalog == null || catalog.isEmpty()) {
            return TriageResult.empty();
        }
        Set<UUID> present = resolveSymptoms(input, catalog);
        List<TriageConditionResult> results = scoreConditions(catalog, present);
        if (results.isEmpty()) {
            return TriageResult.empty();
        }
        double confidence = results.get(0).probability();
        String explanation = "Triaje con " + results.size() + " condición(es) candidata(s) " + "y " + present.size()
                + " síntoma(s) reconocido(s).";
        return new TriageResult(results, List.of(), confidence, explanation, GENERATOR_NAME, ENGINE_VERSION);
    }

    /**
     * Resuelve los síntomas de la entrada contra el catálogo (por id o nombre).
     */
    private Set<UUID> resolveSymptoms(TriageInput input, TriageCatalog catalog) {
        Set<UUID> resolved = new LinkedHashSet<>();
        for (String symptom : input.symptoms()) {
            if (symptom == null || symptom.isBlank()) {
                continue;
            }
            String value = symptom.strip();
            catalog.symptomByName(value).ifPresent(s -> resolved.add(s.id()));
            if (isUuid(value)) {
                catalog.symptomById(UUID.fromString(value)).ifPresent(s -> resolved.add(s.id()));
            }
        }
        return resolved;
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private List<TriageConditionResult> scoreConditions(TriageCatalog catalog, Set<UUID> present) {
        List<RawScore> scored = new ArrayList<>();
        for (Condition condition : catalog.conditions()) {
            RawScore raw = rawScore(catalog, condition, present);
            if (raw != null) {
                scored.add(raw);
            }
        }
        if (scored.isEmpty()) {
            return List.of();
        }
        double total = scored.stream().mapToDouble(RawScore::rawScore).sum();
        if (total <= 0.0) {
            return List.of();
        }
        List<TriageConditionResult> results = new ArrayList<>();
        double sumSoFar = 0.0;
        for (int i = 0; i < scored.size(); i++) {
            boolean isLast = (i == scored.size() - 1);
            TriageConditionResult result = toResult(catalog, scored.get(i), total, isLast, sumSoFar);
            sumSoFar += result.probability();
            results.add(result);
        }
        return results.stream()
                .sorted(Comparator.comparingDouble(TriageConditionResult::probability).reversed())
                .limit(maxConditions)
                .toList();
    }

    /**
     * Calcula el rawScore de una condición; {@code null} si no es candidata
     * (falta un required o no hay síntomas coincidentes).
     */
    private RawScore rawScore(TriageCatalog catalog, Condition condition, Set<UUID> present) {
        List<SymptomConditionRelation> relations = catalog.relationsForCondition(condition.id());
        if (relations.isEmpty()) {
            return null;
        }
        double score = 0.0;
        List<String> matched = new ArrayList<>();
        for (var relation : relations) {
            boolean isPresent = present.contains(relation.symptomId());
            if (relation.required() && !isPresent) {
                return null;
            }
            if (isPresent) {
                score += relation.weight();
                catalog.symptomById(relation.symptomId()).ifPresent(s -> matched.add(s.name()));
            }
        }
        if (score <= 0.0) {
            return null;
        }
        return new RawScore(condition.id(), score, matched);
    }

    private TriageConditionResult toResult(TriageCatalog catalog, RawScore raw, double total, boolean isLast, double sumSoFar) {
        Condition condition = catalog.conditionById(raw.conditionId())
                .orElseThrow(() -> new IllegalStateException("Condición no encontrada: " + raw.conditionId()));
        double probability = raw.rawScore() / total;
        // Ensure probabilities sum to exactly 1.0 by adjusting the last item
        if (isLast) {
            probability = Math.max(0.0, 1.0 - sumSoFar);
        }
        return new TriageConditionResult(
                condition.id(),
                condition.name(),
                condition.description(),
                probability,
                condition.severity(),
                condition.urgency(),
                condition.recommendation(),
                raw.matchedSymptoms());
    }

    private record RawScore(UUID conditionId, double rawScore, List<String> matchedSymptoms) {}
}
