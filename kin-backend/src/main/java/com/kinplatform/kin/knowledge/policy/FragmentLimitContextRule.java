package com.kinplatform.kin.knowledge.policy;

/**
 * Regla de contexto: limita el número de fragmentos del contexto. Un valor
 * {@code 0} en la configuración significa "sin límite".
 *
 * <p>Determinista: comparación de contadores contra la configuración.</p>
 */
public class FragmentLimitContextRule implements ContextRule {

    @Override
    public String name() {
        return "LimiteFragmentos";
    }

    @Override
    public PolicyDecision evaluate(ContextBudget budget, ContextPolicyConfig config) {
        if (config.maxFragments() > 0 && budget.fragmentCount() > config.maxFragments()) {
            return PolicyDecision.reject(PolicyCategory.CONTEXT,
                "Máximo de fragmentos excedido (" + budget.fragmentCount() + "/"
                    + config.maxFragments() + ")");
        }
        return PolicyDecision.allow(PolicyCategory.CONTEXT);
    }
}
