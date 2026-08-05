package com.kinplatform.kin.knowledge.policy;

import com.kinplatform.kin.knowledge.KnowledgeRequest;

/**
 * Regla de consulta: si la caché está habilitada y la ventana temporal pedida
 * cabe dentro del TTL configurado, decide reutilizar únicamente la caché antes
 * de tocar la red. Un miss posterior es resuelto por el orquestador, no por la
 * política.
 *
 * <p>Determinista: comparación de ventanas temporales contra la configuración.</p>
 */
public class CacheFirstQueryRule implements QueryRule {

    @Override
    public String name() {
        return "CachePrimero";
    }

    @Override
    public QueryPolicyResult evaluate(KnowledgeRequest request, QueryPolicyConfig config) {
        if (!config.cacheFirst()) {
            return QueryPolicyResult.of(QueryMode.EXTERNAL, "Caché deshabilitada por configuración");
        }
        if (request.timeWindow() != null && !request.timeWindow().isZero()
            && request.timeWindow().compareTo(config.cacheTtl()) <= 0) {
            return QueryPolicyResult.of(QueryMode.CACHE_ONLY,
                "Ventana temporal dentro del TTL de caché");
        }
        return QueryPolicyResult.of(QueryMode.EXTERNAL, "Ventana temporal amplia; se consulta externamente");
    }
}
