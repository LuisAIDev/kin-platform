package com.kinplatform.kin.knowledge.orchestrator;

import com.kinplatform.kin.knowledge.planner.ProviderType;

import java.util.List;

/**
 * Resultado inmutable del ciclo de orquestación (especificación Fase 5):
 * estado final, estados visitados, plan, decisiones, tipos de proveedor
 * seleccionados, bandera de degradación y motivo de fallo (si existe).
 */
public record OrchestrationResult(
    OrchestrationState finalState,
    List<OrchestrationState> statesVisited,
    OrchestrationPlan plan,
    List<OrchestrationDecision> decisions,
    List<ProviderType> selectedProviderTypes,
    boolean degraded,
    String failureReason
) {

    public OrchestrationResult {
        finalState = finalState == null ? OrchestrationState.COMPLETED : finalState;
        statesVisited = statesVisited == null ? List.of() : List.copyOf(statesVisited);
        plan = plan == null ? new OrchestrationPlan(null, null, null) : plan;
        decisions = decisions == null ? List.of() : List.copyOf(decisions);
        selectedProviderTypes = selectedProviderTypes == null ? List.of() : List.copyOf(selectedProviderTypes);
        failureReason = failureReason == null ? "" : failureReason;
    }

    public boolean completed() {
        return finalState == OrchestrationState.COMPLETED;
    }

    public boolean failed() {
        return finalState == OrchestrationState.FAILED;
    }

    public boolean consulted() {
        return decisions.stream()
            .anyMatch(decision -> decision.type() == OrchestrationDecisionType.CONSULT_EXTERNAL);
    }
}
