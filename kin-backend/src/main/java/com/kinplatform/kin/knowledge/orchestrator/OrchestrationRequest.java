package com.kinplatform.kin.knowledge.orchestrator;

import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.policy.PolicyConfig;

/**
 * Solicitud de orquestación (especificación Fase 5): envuelve el
 * {@link KnowledgeRequest}, la configuración de políticas del entorno, la
 * estrategia de orquestación deseada y el entorno de ejecución. Inmutable.
 */
public record OrchestrationRequest(
    KnowledgeRequest knowledgeRequest,
    PolicyConfig policyConfig,
    OrchestrationStrategy strategy,
    ExecutionEnvironment environment
) {

    public OrchestrationRequest {
        knowledgeRequest = knowledgeRequest == null ? KnowledgeRequest.empty() : knowledgeRequest;
        policyConfig = policyConfig == null ? PolicyConfig.defaults() : policyConfig;
        strategy = strategy == null ? OrchestrationStrategy.GRACEFUL_DEGRADATION : strategy;
        environment = environment == null ? ExecutionEnvironment.online() : environment;
    }

    public static OrchestrationRequest of(KnowledgeRequest knowledgeRequest, PolicyConfig policyConfig,
                                          OrchestrationStrategy strategy, ExecutionEnvironment environment) {
        return new OrchestrationRequest(knowledgeRequest, policyConfig, strategy, environment);
    }

    public static OrchestrationRequest empty() {
        return new OrchestrationRequest(KnowledgeRequest.empty(), PolicyConfig.defaults(),
            OrchestrationStrategy.GRACEFUL_DEGRADATION, ExecutionEnvironment.online());
    }
}
