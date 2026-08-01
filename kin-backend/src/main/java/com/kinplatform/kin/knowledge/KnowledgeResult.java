package com.kinplatform.kin.knowledge;

import com.kinplatform.kin.engine.EngineResult;

import java.util.List;

/**
 * Resultado inmutable del motor de conocimiento (ADR-014): hechos verificados,
 * fuentes utilizadas y validaciones realizadas.
 *
 * <p>Implementa {@link EngineResult} para compartir el contrato común de
 * resultados de la infraestructura de motores sin perder tipado fuerte.
 * {@code empty()} permite el modo offline-first: sin fuentes disponibles, el
 * pipeline sigue operando con un resultado vacío.</p>
 */
public record KnowledgeResult(
    List<KnowledgeFact> facts,
    List<String> sourcesUsed,
    List<SourceValidation> validations,
    double confidence,
    String explanation,
    String generatedBy,
    String engineVersion
) implements EngineResult {

    public KnowledgeResult {
        facts = facts == null ? List.of() : List.copyOf(facts);
        sourcesUsed = sourcesUsed == null ? List.of() : List.copyOf(sourcesUsed);
        validations = validations == null ? List.of() : List.copyOf(validations);
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        explanation = explanation == null ? "" : explanation;
        generatedBy = generatedBy == null ? "" : generatedBy;
        engineVersion = engineVersion == null ? "" : engineVersion;
    }

    public int factCount() {
        return facts.size();
    }

    public static KnowledgeResult empty() {
        return new KnowledgeResult(
            List.of(), List.of(), List.of(), 0.0,
            "No se obtuvo conocimiento externo.", "", "");
    }

    @Override
    public boolean isEmpty() {
        return facts.isEmpty();
    }
}
