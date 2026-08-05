package com.kinplatform.kin.knowledge.citation;

import com.kinplatform.kin.knowledge.KnowledgeFact;

import java.util.Locale;

/**
 * Política de citación por defecto (Fase 5 — Citation Engine): incluye un hecho
 * solo si posee SourceMetadata verificable (sourceId y URL) y su confianza
 * alcanza el umbral mínimo. Nunca cita sin SourceMetadata.
 */
public class VerifiedCitationPolicy implements CitationPolicy {

    private final double minConfidence;

    public VerifiedCitationPolicy() {
        this(0.0);
    }

    public VerifiedCitationPolicy(double minConfidence) {
        this.minConfidence = Math.max(0.0, Math.min(1.0, minConfidence));
    }

    @Override
    public CitationDecision decide(KnowledgeFact fact) {
        if (fact == null) {
            return CitationDecision.excluded("Hecho nulo");
        }
        if (fact.sourceId() == null || fact.sourceId().isBlank()
            || fact.url() == null || fact.url().isBlank()) {
            return CitationDecision.excluded("Sin SourceMetadata verificable (sourceId o URL ausentes)");
        }
        double confidence = CitationConfidence.of(fact.trust());
        if (confidence < minConfidence) {
            return CitationDecision.excluded(
                "Confianza insuficiente para citar: " + percent(confidence) + "%");
        }
        return new CitationDecision(true, fact.sourceId(), fact.url(),
            "Cita verificada: " + fact.trust().displayName() + " (" + percent(confidence) + "%).",
            confidence);
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.0f", value * 100);
    }
}
