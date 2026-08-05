package com.kinplatform.kin.knowledge.policy;

/**
 * Regla de contexto: limita el presupuesto de tokens estimados del contexto.
 * Un valor {@code 0} en la configuración significa "sin límite".
 *
 * <p>Determinista: comparación de contadores contra la configuración.</p>
 */
public class TokenBudgetContextRule implements ContextRule {

    @Override
    public String name() {
        return "PresupuestoTokens";
    }

    @Override
    public PolicyDecision evaluate(ContextBudget budget, ContextPolicyConfig config) {
        if (config.tokenBudget() > 0 && budget.estimatedTokens() > config.tokenBudget()) {
            return PolicyDecision.reject(PolicyCategory.CONTEXT,
                "Presupuesto de tokens excedido (" + budget.estimatedTokens() + "/"
                    + config.tokenBudget() + ")");
        }
        return PolicyDecision.allow(PolicyCategory.CONTEXT);
    }
}
