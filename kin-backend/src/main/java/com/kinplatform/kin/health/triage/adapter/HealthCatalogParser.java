package com.kinplatform.kin.health.triage.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.common.engine.DeterministicId;
import com.kinplatform.kin.health.triage.domain.CatalogUpdate;
import com.kinplatform.kin.health.triage.domain.Condition;
import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.Symptom;
import com.kinplatform.kin.health.triage.domain.SymptomConditionRelation;
import com.kinplatform.kin.health.triage.domain.Urgency;
import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeFact;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Parser de conocimiento médico estructurado → {@link CatalogUpdate} (ADR-028,
 * fase profesional).
 *
 * <p>Convierte el contenido JSON de candidatos/facts del KnowledgeEngine en una
 * {@link CatalogUpdate} de dominio con IDs deterministas
 * ({@link DeterministicId}): el mismo contenido produce siempre el mismo id,
 * haciendo el upsert idempotente. Pertenece a infraestructura (Jackson).</p>
 */
public class HealthCatalogParser {

    private static final String DEFAULT_SOURCE = "health-catalog";

    private final ObjectMapper objectMapper;

    public HealthCatalogParser() {
        this(new ObjectMapper());
    }

    public HealthCatalogParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public CatalogUpdate parseCandidates(List<KnowledgeCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return CatalogUpdate.of(sourceOf(candidates), List.of(), List.of(), List.of());
        }
        var collector = new Collector();
        for (KnowledgeCandidate candidate : candidates) {
            if (candidate == null
                    || candidate.content() == null
                    || candidate.content().isBlank()) {
                continue;
            }
            collect(collector, candidate.content());
        }
        return collector.toUpdate(DEFAULT_SOURCE);
    }

    public CatalogUpdate parseFacts(List<KnowledgeFact> facts) {
        if (facts == null || facts.isEmpty()) {
            return CatalogUpdate.of(DEFAULT_SOURCE, List.of(), List.of(), List.of());
        }
        var collector = new Collector();
        for (KnowledgeFact fact : facts) {
            if (fact == null || fact.claim() == null || fact.claim().isBlank()) {
                continue;
            }
            collect(collector, fact.claim());
        }
        return collector.toUpdate(DEFAULT_SOURCE);
    }

    private void collect(Collector collector, String content) {
        try {
            HealthCatalogEntry entry = objectMapper.readValue(content, HealthCatalogEntry.class);
            if (entry != null && entry.getName() != null && !entry.getName().isBlank()) {
                collector.add(entry);
            }
        } catch (JsonProcessingException ex) {
            // Contenido no estructurado (texto libre del KnowledgeEngine): se ignora
            // para el catálogo; el pipeline de triaje nunca depende de esto.
        }
    }

    private static String sourceOf(List<KnowledgeCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return DEFAULT_SOURCE;
        }
        for (KnowledgeCandidate candidate : candidates) {
            if (candidate != null
                    && candidate.sourceId() != null
                    && !candidate.sourceId().isBlank()) {
                return candidate.sourceId();
            }
        }
        return DEFAULT_SOURCE;
    }

    /** Acumula entradas y produce una {@link CatalogUpdate} deduplicada. */
    private static final class Collector {

        private final Map<String, Symptom> symptoms = new LinkedHashMap<>();
        private final Map<String, Condition> conditions = new LinkedHashMap<>();
        private final List<SymptomConditionRelation> relations = new ArrayList<>();

        void add(HealthCatalogEntry entry) {
            Condition condition = toCondition(entry);
            conditions.putIfAbsent(condition.name(), condition);
            if (entry.getSymptoms() == null) {
                return;
            }
            for (HealthCatalogEntry.SymptomRef ref : entry.getSymptoms()) {
                if (ref == null || ref.getName() == null || ref.getName().isBlank()) {
                    continue;
                }
                Symptom symptom = toSymptom(ref.getName());
                symptoms.putIfAbsent(symptom.name(), symptom);
                relations.add(
                        SymptomConditionRelation.of(symptom.id(), condition.id(), ref.getWeight(), ref.isRequired()));
            }
        }

        CatalogUpdate toUpdate(String source) {
            return CatalogUpdate.of(
                    source, List.copyOf(symptoms.values()), List.copyOf(conditions.values()), List.copyOf(relations));
        }

        private static Symptom toSymptom(String name) {
            UUID id = DeterministicId.from("triage-symptom", name, "");
            return Symptom.of(id, name, "", null, List.of());
        }

        private static Condition toCondition(HealthCatalogEntry entry) {
            UUID id = DeterministicId.from("triage-condition", entry.getName(), entry.getIcdCode());
            return Condition.of(
                    id,
                    entry.getName(),
                    entry.getDescription() == null ? "" : entry.getDescription(),
                    entry.getIcdCode(),
                    parseSeverity(entry.getSeverity()),
                    parseUrgency(entry.getUrgency()),
                    entry.getRecommendation() == null ? "" : entry.getRecommendation());
        }

        private static Severity parseSeverity(String raw) {
            if (raw == null) {
                return Severity.LEVE;
            }
            try {
                return Severity.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return Severity.LEVE;
            }
        }

        private static Urgency parseUrgency(String raw) {
            if (raw == null) {
                return Urgency.BAJA;
            }
            try {
                return Urgency.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return Urgency.BAJA;
            }
        }
    }
}

