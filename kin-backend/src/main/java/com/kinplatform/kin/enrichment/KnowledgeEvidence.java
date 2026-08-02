package com.kinplatform.kin.enrichment;

import com.kinplatform.kin.knowledge.KnowledgeFact;

/**
 * Evidencia de conocimiento seleccionada para el análisis (ADR-016). Inmutable.
 *
 * <p>Une un hecho verificado ({@link KnowledgeFact}) con el score de relevancia
 * calculado en Java para una categoría. Representa "este hecho externo sustenta
 * esta parte del análisis, con esta confianza".</p>
 */
public record KnowledgeEvidence(
    KnowledgeFact fact,
    EvidenceScore score
) {

    public KnowledgeEvidence {
        fact = fact == null ? null : fact;
        score = score == null ? EvidenceScore.of(0.0, null, "") : score;
    }

    public EvidenceCategory category() {
        return score != null ? score.category() : null;
    }

    public double scoreValue() {
        return score != null ? score.value() : 0.0;
    }
}
