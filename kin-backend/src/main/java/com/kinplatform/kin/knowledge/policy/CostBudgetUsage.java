package com.kinplatform.kin.knowledge.policy;

/**
 * Uso inmutable del presupuesto del turno (especificación Fase 2): consultas
 * consumidas y llamadas externas consumidas, insumo de las políticas de costo.
 */
public record CostBudgetUsage(
    int consumedQueries,
    int consumedExternalCalls
) {

    public CostBudgetUsage {
        consumedQueries = Math.max(0, consumedQueries);
        consumedExternalCalls = Math.max(0, consumedExternalCalls);
    }
}
