package com.kinplatform.kin.enrichment;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;

import java.util.List;

/**
 * Motor canonizado de enriquecimiento con conocimiento externo (ADR-016,
 * ADR-005/009).
 *
 * <p>Implementa {@link DomainEngine} (fase {@code ANALYSIS}, tipo {@code DOMAIN},
 * prioridad 55) para integrarse con la infraestructura común de motores sin
 * modificar su lógica. Recibe un {@link EnrichmentInput}, ranquea la evidencia
 * mediante el {@link FactRanker} y produce exclusivamente un
 * {@link EnrichmentResult}.</p>
 *
 * <p>El motor nunca habla con un LLM, nunca construye prompts y nunca depende de
 * infraestructura: la relevancia, el ranking y la confianza son decisiones
 * deterministas de Java (principio "Java decide. El LLM únicamente comunica.").</p>
 */
public class EnrichmentEngine implements DomainEngine<EnrichmentInput, EnrichmentResult> {

    public static final String GENERATOR_NAME = "EnrichmentEngine";
    public static final String ENGINE_VERSION = "v1";

    private final FactRanker ranker;

    public EnrichmentEngine(FactRanker ranker) {
        this.ranker = ranker;
    }

    @Override
    public EngineMetadata metadata() {
        return EngineMetadata.of(GENERATOR_NAME, ENGINE_VERSION, "KIN Architecture Team",
            EnginePhase.ANALYSIS, EngineType.DOMAIN, 55);
    }

    @Override
    public EnrichmentResult evaluate(EnrichmentInput input) {
        if (input == null || input.context() == null || input.knowledge() == null || ranker == null) {
            return EnrichmentResult.empty();
        }
        if (input.knowledge().isEmpty()) {
            return EnrichmentResult.empty();
        }
        List<EvidenceRank> ranks = ranker.rank(input);
        List<String> sourcesUsed = ranks.stream()
            .flatMap(r -> r.evidence().stream())
            .map(e -> e.fact().sourceId())
            .distinct()
            .sorted()
            .toList();
        double confidence = ranks.isEmpty() ? 0.0
            : ranks.stream().mapToDouble(EvidenceRank::confidence).average().orElse(0.0);
        String explanation = ranks.isEmpty()
            ? "No se enriqueció el análisis."
            : "Análisis enriquecido con " + ranks.stream().mapToInt(EvidenceRank::size).sum()
                + " evidencia(s) de conocimiento externo.";
        return new EnrichmentResult(ranks, sourcesUsed, confidence, explanation,
            GENERATOR_NAME, ENGINE_VERSION);
    }
}
