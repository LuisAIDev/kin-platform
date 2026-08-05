package com.kinplatform.kin.knowledge.policy;

/**
 * Regla de costo: limita el número de consultas por turno. Si el límite está
 * en {@code 0} (sin límite configurado), no restringe.
 *
 * <p>Determinista: comparación de contadores contra la configuración.</p>
 */
public class QueryBudgetCostRule implements CostRule {

    @Override
    public String name() {
        return "PresupuestoConsultas";
    }

    @Override
    public PolicyDecision evaluate(CostBudgetUsage usage, CostPolicyConfig config) {
        if (config.maxQueries() > 0 && usage.consumedQueries() >= config.maxQueries()) {
            return PolicyDecision.reject(PolicyCategory.COST,
                "Presupuesto de consultas agotado (" + usage.consumedQueries() + "/"
                    + config.maxQueries() + ")");
        }
        return PolicyDecision.allow(PolicyCategory.COST);
    }
}
