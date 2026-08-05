package com.kinplatform.kin.knowledge.orchestrator;

import com.kinplatform.kin.knowledge.KnowledgeResult;

import java.util.List;

/**
 * Resultado inmutable de la orquestación con ejecución física (integración):
 * envuelve la decisión de orquestación, el conocimiento adquirido y la bandera
 * de reutilización de caché.
 */
public record KnowledgeOrchestrationResult(
    OrchestrationResult orchestration,
    KnowledgeResult knowledge,
    boolean cacheHit
) {

    public KnowledgeOrchestrationResult {
        orchestration = orchestration == null
            ? new OrchestrationResult(OrchestrationState.FAILED, List.of(), null, List.of(), List.of(), false, "")
            : orchestration;
        knowledge = knowledge == null ? KnowledgeResult.empty() : knowledge;
    }
}
