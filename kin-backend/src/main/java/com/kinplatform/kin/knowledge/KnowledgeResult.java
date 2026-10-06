package com.kinplatform.kin.knowledge;

import com.kinplatform.common.engine.EngineResult;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

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
        String engineVersion)
        implements EngineResult {

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

    /**
     * TTL efectivo de caché (ADR-025): el mínimo {@code maxAge} de las fuentes
     * que aportaron hechos (la fuente que cambia más seguido fija la expiración).
     * Vacío si ningún hecho declaró {@code maxAge} (el llamador usa la ventana
     * de la solicitud).
     */
    public Optional<Duration> effectiveTtl() {
        Duration min = null;
        for (KnowledgeFact fact : facts) {
            if (fact.maxAge() != null && (min == null || fact.maxAge().compareTo(min) < 0)) {
                min = fact.maxAge();
            }
        }
        return Optional.ofNullable(min);
    }

    public static KnowledgeResult empty() {
        return new KnowledgeResult(List.of(), List.of(), List.of(), 0.0, "No se obtuvo conocimiento externo.", "", "");
    }

    @Override
    public boolean isEmpty() {
        return facts.isEmpty();
    }
}

