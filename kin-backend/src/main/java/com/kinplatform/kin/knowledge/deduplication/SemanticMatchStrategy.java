package com.kinplatform.kin.knowledge.deduplication;

import com.kinplatform.kin.knowledge.KnowledgeFact;

/**
 * Estrategia semántica usando embeddings (stub para futura implementación).
 *
 * <p>Cuando se implemente, usará embeddings vectoriales (p.ej. sentence-transformers)
 * para comparar la similitud semántica de los claims.</p>
 */
public class SemanticMatchStrategy implements DeduplicationStrategy {

    public static final String STRATEGY_NAME = "SEMANTIC";

    private final double threshold;

    public SemanticMatchStrategy() {
        this(0.90);
    }

    public SemanticMatchStrategy(double threshold) {
        this.threshold = Math.max(0.0, Math.min(1.0, threshold));
    }

    @Override
    public String strategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public int priority() {
        return 30; // Tercera prioridad
    }

    @Override
    public boolean areDuplicates(KnowledgeFact a, KnowledgeFact b) {
        if (a == null || b == null) {
            return false;
        }
        if (a == b) {
            return true;
        }
        if (!a.sourceId().equals(b.sourceId())) {
            return false;
        }
        if (!a.category().equals(b.category())) {
            return false;
        }
        // TODO: Implementar con embeddings vectoriales
        // Por ahora, delega a fuzzy match como fallback
        return false;
    }

    /**
     * Placeholder para futura implementación con embeddings.
     * Cuando se implemente, comparará embeddings vectoriales de los claims
     * usando similitud de coseno.
     */
    protected double semanticSimilarity(String claimA, String claimB) {
        // TODO: Implementar con embeddings (sentence-transformers, etc.)
        return 0.0;
    }
}