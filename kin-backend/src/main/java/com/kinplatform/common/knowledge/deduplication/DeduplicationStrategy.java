package com.kinplatform.common.knowledge.deduplication;

import com.kinplatform.common.knowledge.KnowledgeFact;

/**
 * Estrategia de deduplicación: determina si dos hechos son duplicados.
 */
public interface DeduplicationStrategy {

    /**
     * Determina si dos hechos son considerados duplicados.
     *
     * @param a primer hecho
     * @param b segundo hecho
     * @return true si son considerados duplicados
     */
    boolean areDuplicates(KnowledgeFact a, KnowledgeFact b);

    /**
     * Nombre identificador de la estrategia (p.ej. "EXACT_MATCH", "FUZZY_MATCH").
     */
    String strategyName();

    /**
     * Prioridad de la estrategia: menor valor = mayor prioridad (se ejecuta primero).
     */
    default int priority() {
        return 0;
    }
}

