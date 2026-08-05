package com.kinplatform.kin.knowledge.policy;

/**
 * Regla de costo: limita el número de llamadas externas por turno (APIs,
 * Internet). Si el límite está en {@code 0} (sin límite configurado), no
 * restringe.
 *
 * <p>Determinista: comparación de contadores contra la configuración.</p>
 */
public class ExternalCallCostRule implements CostRule {

    @Override
    public String name() {
        return "LimiteLlamadasExternas";
    }

    @Override
    public PolicyDecision evaluate(CostBudgetUsage usage, CostPolicyConfig config) {
        if (config.maxExternalCalls() > 0 && usage.consumedExternalCalls() >= config.maxExternalCalls()) {
            return PolicyDecision.reject(PolicyCategory.COST,
                "Límite de llamadas externas alcanzado (" + usage.consumedExternalCalls() + "/"
                    + config.maxExternalCalls() + ")");
        }
        return PolicyDecision.allow(PolicyCategory.COST);
    }
}
