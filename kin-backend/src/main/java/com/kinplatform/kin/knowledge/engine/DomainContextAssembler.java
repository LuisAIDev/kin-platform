package com.kinplatform.kin.knowledge.engine;

import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.SourceTrust;
import com.kinplatform.kin.knowledge.orchestrator.ContextAssembler;
import com.kinplatform.kin.knowledge.orchestrator.RankedCandidate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Ensamblado del resultado de conocimiento (integración física): recibe
 * únicamente candidatos ya rankeados y produce un {@link KnowledgeResult}
 * inmutable — normalización de hechos, confianza determinista y explicación —
 * replicando exactamente las métricas del núcleo congelado (ADR-014).
 *
 * <p>Stampa generador y versión del motor por inyección (sin depender del motor
 * directamente) y fabrica resultados vacíos con motivo auditable (offline-first).</p>
 */
public class DomainContextAssembler implements ContextAssembler {

    private final String generatorName;
    private final String engineVersion;

    public DomainContextAssembler() {
        this(KnowledgeEngine.GENERATOR_NAME, KnowledgeEngine.ENGINE_VERSION);
    }

    public DomainContextAssembler(String generatorName, String engineVersion) {
        this.generatorName = generatorName == null ? KnowledgeEngine.GENERATOR_NAME : generatorName;
        this.engineVersion = engineVersion == null ? KnowledgeEngine.ENGINE_VERSION : engineVersion;
    }

    @Override
    public KnowledgeResult assemble(KnowledgeQuery query, List<RankedCandidate> ranked) {
        if (ranked == null || ranked.isEmpty()) {
            return emptyResult("No se obtuvieron candidatos de las fuentes registradas.");
        }
        var facts = new ArrayList<KnowledgeFact>();
        var used = new LinkedHashSet<String>();
        for (var pair : ranked) {
            if (pair.validation().accepted()) {
                facts.add(normalize(pair, pair.validation().trust()));
                used.add(pair.candidate().sourceId());
            }
        }
        var factsCopy = List.copyOf(facts);
        var sourcesUsed = List.copyOf(new ArrayList<>(used));
        double confidence = computeConfidence(factsCopy, ranked.size());
        String explanation = buildExplanation(factsCopy, ranked.size(), confidence);
        return new KnowledgeResult(factsCopy, sourcesUsed, validationsOf(ranked), confidence,
            explanation, generatorName, engineVersion);
    }

    @Override
    public KnowledgeResult emptyResult(String reason) {
        return new KnowledgeResult(List.of(), List.of(), List.of(), 0.0,
            reason == null ? "" : reason, generatorName, engineVersion);
    }

    private KnowledgeFact normalize(RankedCandidate pair, SourceTrust trust) {
        var candidate = pair.candidate();
        String category = candidate.meta().getOrDefault(SourceValidator.META_CATEGORY, "");
        return KnowledgeFact.of(candidate.content().strip(), candidate.sourceId(), candidate.url(),
            candidate.publishedAt(), trust, category);
    }

    private List<com.kinplatform.kin.knowledge.SourceValidation> validationsOf(List<RankedCandidate> ranked) {
        var validations = new ArrayList<com.kinplatform.kin.knowledge.SourceValidation>();
        for (var pair : ranked) {
            validations.add(pair.validation());
        }
        return List.copyOf(validations);
    }

    private double computeConfidence(List<KnowledgeFact> facts, int totalCandidates) {
        if (facts.isEmpty()) {
            return 0.0;
        }
        double acceptance = (double) facts.size() / totalCandidates;
        double avgTrust = facts.stream()
            .mapToDouble(fact -> trustWeight(fact.trust()))
            .average().orElse(0.0);
        double quality = 0.5 * acceptance + 0.5 * avgTrust;
        double contentQuality = facts.stream()
            .mapToDouble(fact -> contentQuality(fact.claim()))
            .average().orElse(0.0);
        return Math.max(0.0, Math.min(1.0, 0.6 * quality + 0.4 * contentQuality));
    }

    private double trustWeight(SourceTrust trust) {
        return switch (trust) {
            case OFFICIAL_PUBLIC -> 1.0;
            case SECONDARY -> 0.7;
            case UNVERIFIED -> 0.4;
        };
    }

    private double contentQuality(String claim) {
        return Math.min(1.0, claim == null ? 0.0 : claim.length() / 200.0);
    }

    private String buildExplanation(List<KnowledgeFact> facts, int totalCandidates, double confidence) {
        int accepted = facts.size();
        int rejected = totalCandidates - accepted;
        String qualityPercent = String.format(Locale.ROOT, "%.0f", confidence * 100);
        return "Candidatos aceptados: " + accepted + " de " + totalCandidates
            + " (" + rejected + " descartados). Calidad: " + qualityPercent + "%.";
    }
}
