package com.kinplatform.kin.knowledge.policy;

import com.kinplatform.kin.knowledge.KnowledgeRequest;

import java.util.Locale;

/**
 * Regla de consulta: si el tema del request es un tema estable configurado
 * (p. ej. "scrum", "kanban"), decide responder únicamente con el conocimiento
 * del modelo, sin consultar fuentes externas (cero latencia y cero costo).
 *
 * <p>Determinista: comparación normalizada contra la configuración inyectada.</p>
 */
public class StableTopicQueryRule implements QueryRule {

    @Override
    public String name() {
        return "TemaEstable";
    }

    @Override
    public QueryPolicyResult evaluate(KnowledgeRequest request, QueryPolicyConfig config) {
        if (config.stableTopics().isEmpty()) {
            return QueryPolicyResult.of(QueryMode.EXTERNAL, "Sin temas estables configurados");
        }
        String normalized = request.topic().trim().toLowerCase(Locale.ROOT);
        if (config.stableTopics().contains(normalized)) {
            return QueryPolicyResult.of(QueryMode.MODEL_ONLY,
                "Tema estable; responde el modelo sin consulta externa");
        }
        return QueryPolicyResult.of(QueryMode.EXTERNAL, "Tema no estable");
    }
}
