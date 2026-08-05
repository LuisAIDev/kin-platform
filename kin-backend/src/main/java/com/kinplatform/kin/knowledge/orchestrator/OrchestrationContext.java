package com.kinplatform.kin.knowledge.orchestrator;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.SourceValidation;
import com.kinplatform.kin.knowledge.policy.ProviderSelection;
import com.kinplatform.kin.knowledge.planner.ProviderType;

import java.util.ArrayList;
import java.util.List;

/**
 * Contexto mutable del ciclo de orquestación (especificación Fase 5). Es el
 * ÚNICO objeto mutable del Knowledge Engine: acumula estado interno (estado
 * actual, estados visitados, decisiones, plan, selección de proveedores,
 * motivos y, en la integración física, los artefactos de ejecución) mientras
 * el orquestador recorre la máquina de estados. Todos los contratos de salida
 * son inmutables.
 */
public final class OrchestrationContext {

    private final OrchestrationRequest request;
    private OrchestrationState currentState;
    private final List<OrchestrationState> visited = new ArrayList<>();
    private final List<OrchestrationDecision> decisions = new ArrayList<>();
    private OrchestrationPlan plan;
    private ProviderSelection providerSelection;
    private List<ProviderType> selectedTypes = List.of();
    private boolean degraded;
    private String failureReason = "";
    private List<KnowledgeCandidate> candidates = List.of();
    private List<SourceValidation> validations = List.of();
    private List<RankedCandidate> ranked = List.of();
    private KnowledgeResult knowledgeResult;
    private boolean cacheHit;

    OrchestrationContext(OrchestrationRequest request) {
        this.request = request == null ? OrchestrationRequest.empty() : request;
    }

    OrchestrationRequest request() {
        return request;
    }

    OrchestrationState currentState() {
        return currentState;
    }

    void transition(OrchestrationState state) {
        if (state == null) {
            state = OrchestrationState.FAILED;
        }
        if (currentState != null && !OrchestrationState.canTransition(currentState, state)) {
            currentState = OrchestrationState.FAILED;
            return;
        }
        currentState = state;
        visited.add(state);
    }

    List<OrchestrationState> visited() {
        return List.copyOf(visited);
    }

    void addDecision(OrchestrationDecision decision) {
        if (decision != null) {
            decisions.add(decision);
        }
    }

    List<OrchestrationDecision> decisions() {
        return List.copyOf(decisions);
    }

    OrchestrationPlan plan() {
        return plan;
    }

    void setPlan(OrchestrationPlan plan) {
        this.plan = plan;
    }

    ProviderSelection providerSelection() {
        return providerSelection;
    }

    void setProviderSelection(ProviderSelection providerSelection) {
        this.providerSelection = providerSelection;
    }

    List<ProviderType> selectedTypes() {
        return List.copyOf(selectedTypes);
    }

    void setSelectedTypes(List<ProviderType> types) {
        this.selectedTypes = types == null ? List.of() : List.copyOf(types);
    }

    boolean degraded() {
        return degraded;
    }

    void markDegraded() {
        this.degraded = true;
    }

    String failureReason() {
        return failureReason;
    }

    void setFailureReason(String reason) {
        this.failureReason = reason == null ? "" : reason;
    }

    List<KnowledgeCandidate> candidates() {
        return safeCopy(candidates);
    }

    void setCandidates(List<KnowledgeCandidate> candidates) {
        this.candidates = candidates == null ? List.of() : new ArrayList<>(candidates);
    }

    List<SourceValidation> validations() {
        return safeCopy(validations);
    }

    void setValidations(List<SourceValidation> validations) {
        this.validations = validations == null ? List.of() : new ArrayList<>(validations);
    }

    List<RankedCandidate> ranked() {
        return safeCopy(ranked);
    }

    void setRanked(List<RankedCandidate> ranked) {
        this.ranked = ranked == null ? List.of() : new ArrayList<>(ranked);
    }

    KnowledgeResult knowledgeResult() {
        return knowledgeResult;
    }

    void setKnowledgeResult(KnowledgeResult knowledgeResult) {
        this.knowledgeResult = knowledgeResult;
    }

    boolean cacheHit() {
        return cacheHit;
    }

    void setCacheHit(boolean cacheHit) {
        this.cacheHit = cacheHit;
    }

    /**
     * Copia defensiva que tolera elementos nulos (un proveedor puede devolver un
     * candidato nulo, que la validación descarta).
     */
    private static <T> List<T> safeCopy(List<T> values) {
        return values == null || values.isEmpty()
            ? List.of()
            : java.util.Collections.unmodifiableList(new ArrayList<>(values));
    }
}
