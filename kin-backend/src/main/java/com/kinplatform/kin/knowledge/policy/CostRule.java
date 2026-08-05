package com.kinplatform.kin.knowledge.policy;

/**
 * Contrato tipado de una regla de política de costo (Strategy Pattern): evalúa
 * el uso del presupuesto contra la configuración y decide si aún se permite
 * gastar. Nunca ejecuta llamadas: solo responde decisiones.
 */
public interface CostRule extends PolicyRule {

    @Override
    default PolicyCategory category() {
        return PolicyCategory.COST;
    }

    PolicyDecision evaluate(CostBudgetUsage usage, CostPolicyConfig config);
}
