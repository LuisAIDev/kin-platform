package com.kinplatform.kin.knowledge.orchestrator;

/**
 * Decisión inmutable emitida por el Knowledge Orchestrator en un estado
 * concreto, con su motivo auditable (especificación Fase 5).
 */
public record OrchestrationDecision(
    OrchestrationDecisionType type,
    OrchestrationState state,
    String reason
) {

    public OrchestrationDecision {
        type = type == null ? OrchestrationDecisionType.CONSULT_EXTERNAL : type;
        state = state == null ? OrchestrationState.PLANNING : state;
        reason = reason == null ? "" : reason;
    }

    public static OrchestrationDecision of(OrchestrationDecisionType type, OrchestrationState state,
                                           String reason) {
        return new OrchestrationDecision(type, state, reason);
    }
}
