package com.kinplatform.kin.knowledge.policy;

/**
 * Uso inmutable del contexto del turno (especificación Fase 2): fragmentos
 * acumulados, tokens estimados y tamaño estimado del contexto, insumo de las
 * políticas de contexto.
 */
public record ContextBudget(
    int fragmentCount,
    int estimatedTokens,
    int estimatedSize
) {

    public ContextBudget {
        fragmentCount = Math.max(0, fragmentCount);
        estimatedTokens = Math.max(0, estimatedTokens);
        estimatedSize = Math.max(0, estimatedSize);
    }
}
