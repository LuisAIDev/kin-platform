package com.kinplatform.kin.knowledge.engine;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.SourceTrust;
import com.kinplatform.kin.knowledge.SourceValidation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Coordinador de la adquisición de conocimiento externo (ADR-014, §5.2).
 *
 * <p>Únicamente orquesta: deriva la {@link KnowledgeQuery} del
 * {@link KnowledgeRequest}, consulta las fuentes del {@link SourceRegistry},
 * delega la validación en el {@link SourceValidator}, normaliza los candidatos
 * aceptados en {@link KnowledgeFact} y agrega las métricas deterministas
 * (confianza, calidad, fuentes aceptadas y descartadas) para construir el
 * {@link KnowledgeResult}.</p>
 *
 * <p>No contiene reglas de negocio (protocolo, allowlist, estado, frescura,
 * formato, deduplicación y confianza viven en {@link SourceValidator}), no
 * toca la red ni conoce Internet: el dominio solo consume el puerto
 * {@link com.kinplatform.kin.knowledge.KnowledgeSource}.</p>
 */
public class KnowledgeGateway {

    private final SourceRegistry registry;
    private final SourceValidator validator;

    /**
     * @param registry  registro de fuentes; si es {@code null} se usa uno vacío
     * @param validator validador de candidatos; si es {@code null} se usa el
     *                  validador estricto (offline-first)
     */
    public KnowledgeGateway(SourceRegistry registry, SourceValidator validator) {
        this.registry = registry == null ? SourceRegistry.empty() : registry;
        this.validator = validator == null ? SourceValidator.strict() : validator;
    }

    /**
     * Adquiere y normaliza conocimiento para una {@link KnowledgeRequest},
     * calculando determinísticamente confianza y calidad. Sin red, degrada con
     * gracia a un resultado vacío (offline-first).
     */
    public KnowledgeResult acquire(KnowledgeRequest request) {
        if (request == null || request.topic() == null || request.topic().isBlank()) {
            return emptyResult("Tema vacío; no se consultaron fuentes.");
        }
        var query = KnowledgeQuery.from(request);
        var candidates = collectCandidates(query);
        if (candidates.isEmpty()) {
            return emptyResult("No se obtuvieron candidatos de las fuentes registradas.");
        }
        var validations = validator.validateAll(candidates);
        var facts = new ArrayList<KnowledgeFact>();
        var used = new LinkedHashSet<String>();
        for (int i = 0; i < candidates.size(); i++) {
            if (validations.get(i).accepted()) {
                facts.add(normalize(candidates.get(i), validations.get(i).trust()));
                used.add(candidates.get(i).sourceId());
            }
        }
        var factsCopy = List.copyOf(facts);
        var sourcesUsed = List.copyOf(new ArrayList<>(used));
        var validationsCopy = List.copyOf(validations);
        double confidence = computeConfidence(factsCopy, candidates.size());
        String explanation = buildExplanation(factsCopy, validationsCopy, candidates.size(), confidence);
        return new KnowledgeResult(factsCopy, sourcesUsed, validationsCopy, confidence,
            explanation, KnowledgeEngine.GENERATOR_NAME, KnowledgeEngine.ENGINE_VERSION);
    }

    private List<KnowledgeCandidate> collectCandidates(KnowledgeQuery query) {
        var all = new ArrayList<KnowledgeCandidate>();
        for (var source : registry.all()) {
            var fetched = source.fetch(query);
            if (fetched != null) {
                all.addAll(fetched);
            }
        }
        return all;
    }

    private KnowledgeFact normalize(KnowledgeCandidate candidate, SourceTrust trust) {
        String category = candidate.meta().getOrDefault(SourceValidator.META_CATEGORY, "");
        return KnowledgeFact.of(candidate.content().strip(), candidate.sourceId(), candidate.url(),
            candidate.publishedAt(), trust, category);
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
        double confidence = 0.6 * quality + 0.4 * contentQuality;
        return Math.max(0.0, Math.min(1.0, confidence));
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

    private String buildExplanation(List<KnowledgeFact> facts, List<SourceValidation> validations,
                                    int totalCandidates, double confidence) {
        int accepted = facts.size();
        int rejected = totalCandidates - accepted;
        String qualityPercent = String.format(Locale.ROOT, "%.0f", confidence * 100);
        return "Candidatos aceptados: " + accepted + " de " + totalCandidates
            + " (" + rejected + " descartados). Calidad: " + qualityPercent + "%.";
    }

    private KnowledgeResult emptyResult(String reason) {
        return new KnowledgeResult(List.of(), List.of(), List.of(), 0.0,
            reason, KnowledgeEngine.GENERATOR_NAME, KnowledgeEngine.ENGINE_VERSION);
    }
}
