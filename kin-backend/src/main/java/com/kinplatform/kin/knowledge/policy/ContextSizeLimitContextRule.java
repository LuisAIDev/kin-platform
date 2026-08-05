package com.kinplatform.kin.knowledge.policy;

/**
 * Regla de contexto: limita el tamaño estimado (bytes/caracteres) del
 * contexto. Un valor {@code 0} en la configuración significa "sin límite".
 *
 * <p>Determinista: comparación de contadores contra la configuración.</p>
 */
public class ContextSizeLimitContextRule implements ContextRule {

    @Override
    public String name() {
        return "LimiteTamanoContexto";
    }

    @Override
    public PolicyDecision evaluate(ContextBudget budget, ContextPolicyConfig config) {
        if (config.maxContextSize() > 0 && budget.estimatedSize() > config.maxContextSize()) {
            return PolicyDecision.reject(PolicyCategory.CONTEXT,
                "Tamaño de contexto excedido (" + budget.estimatedSize() + "/"
                    + config.maxContextSize() + ")");
        }
        return PolicyDecision.allow(PolicyCategory.CONTEXT);
    }
}
