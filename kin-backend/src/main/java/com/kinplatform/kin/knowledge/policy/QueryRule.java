package com.kinplatform.kin.knowledge.policy;

import com.kinplatform.kin.knowledge.KnowledgeRequest;

/**
 * Contrato tipado de una regla de política de consulta (Strategy Pattern):
 * evalúa un {@link KnowledgeRequest} contra la configuración y decide un modo
 * de adquisición. Nunca ejecuta consultas: solo responde decisiones.
 */
public interface QueryRule extends PolicyRule {

    @Override
    default PolicyCategory category() {
        return PolicyCategory.QUERY;
    }

    QueryPolicyResult evaluate(KnowledgeRequest request, QueryPolicyConfig config);
}
