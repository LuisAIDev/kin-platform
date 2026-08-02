package com.kinplatform.kin.enrichment;

import com.kinplatform.kin.engine.EngineResult;

import java.util.List;
import java.util.Optional;

/**
 * Resultado inmutable del motor de enriquecimiento (ADR-016): rankings de
 * evidencia por categoría, fuentes utilizadas y confianza agregada.
 *
 * <p>Implementa {@link EngineResult} para compartir el contrato común de
 * resultados. {@code empty()} permite el modo offline-first: sin hechos
 * verificados, el pipeline sigue operando con un resultado vacío.</p>
 */
public record EnrichmentResult(
    List<EvidenceRank> ranks,
    List<String> sourcesUsed,
    double confidence,
    String explanation,
    String generatedBy,
    String engineVersion
) implements EngineResult {

    public EnrichmentResult {
        ranks = ranks == null ? List.of() : List.copyOf(ranks);
        sourcesUsed = sourcesUsed == null ? List.of() : List.copyOf(sourcesUsed);
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        explanation = explanation == null ? "" : explanation;
        generatedBy = generatedBy == null ? "" : generatedBy;
        engineVersion = engineVersion == null ? "" : engineVersion;
    }

    public int totalEvidence() {
        return ranks.stream().mapToInt(EvidenceRank::size).sum();
    }

    public int rankCount() {
        return ranks.size();
    }

    public Optional<EvidenceRank> rankFor(EvidenceCategory category) {
        return ranks.stream()
            .filter(r -> category.equals(r.category()))
            .findFirst();
    }

    public static EnrichmentResult empty() {
        return new EnrichmentResult(
            List.of(), List.of(), 0.0,
            "No se pudo enriquecer el análisis.", "", "");
    }

    @Override
    public boolean isEmpty() {
        return totalEvidence() == 0;
    }
}
