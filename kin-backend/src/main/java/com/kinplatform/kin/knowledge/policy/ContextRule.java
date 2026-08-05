package com.kinplatform.kin.knowledge.policy;

/**
 * Contrato tipado de una regla de política de contexto (Strategy Pattern):
 * evalúa el uso del contexto contra la configuración y decide si aún cabe
 * dentro del presupuesto que podrá consumir la construcción del prompt.
 */
public interface ContextRule extends PolicyRule {

    @Override
    default PolicyCategory category() {
        return PolicyCategory.CONTEXT;
    }

    PolicyDecision evaluate(ContextBudget budget, ContextPolicyConfig config);
}
