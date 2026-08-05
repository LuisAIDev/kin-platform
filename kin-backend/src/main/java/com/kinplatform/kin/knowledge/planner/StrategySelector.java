package com.kinplatform.kin.knowledge.planner;

import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.policy.KnowledgePolicyEngine;
import com.kinplatform.kin.knowledge.policy.QueryMode;
import com.kinplatform.kin.knowledge.policy.QueryPolicyConfig;

import java.util.Set;

/**
 * Tercer paso del pipeline del Query Planner (especificación Fase 3): decide la
 * estrategia ({@link QueryStrategy}) consumiendo el {@link KnowledgePolicyEngine}
 * únicamente mediante su interfaz de dominio y reglas deterministas. Nunca
 * ejecuta consultas ni conoce proveedores.
 *
 * <p>Reglas deterministas:</p>
 * <ul>
 *   <li>Conocimiento estable → {@code SINGLE} (sin consultas).</li>
 *   <li>Modo de consulta del Policy Engine: {@code MODEL_ONLY} → SINGLE;
 *       {@code CACHE_ONLY} → CACHED.</li>
 *   <li>Solo facetas locales (documento/RAG) → LOCAL_ONLY.</li>
 *   <li>Faceta externa única: TENDENCIAS → INTERNET_ONLY; resto → SINGLE.</li>
 *   <li>Local + externa o varios dominios distintos → HYBRID.</li>
 *   <li>Un solo dominio con REGULATORIA+LEGAL → SEQUENTIAL.</li>
 *   <li>Un solo dominio con dos facetas → MULTI.</li>
 *   <li>Un solo dominio con tres o más facetas → PARALLEL.</li>
 * </ul>
 */
public class StrategySelector {

    private final KnowledgePolicyEngine policyEngine;
    private final QueryPolicyConfig queryConfig;

    public StrategySelector() {
        this(new KnowledgePolicyEngine(), QueryPolicyConfig.defaults());
    }

    public StrategySelector(KnowledgePolicyEngine policyEngine, QueryPolicyConfig queryConfig) {
        this.policyEngine = policyEngine == null ? new KnowledgePolicyEngine() : policyEngine;
        this.queryConfig = queryConfig == null ? QueryPolicyConfig.defaults() : queryConfig;
    }

    public QueryStrategy select(QueryClassification classification, KnowledgeRequest request) {
        if (classification == null) {
            return QueryStrategy.SINGLE;
        }
        if (classification.type() == IntentType.CONOCIMIENTO_ESTABLE) {
            return QueryStrategy.SINGLE;
        }
        QueryMode mode = policyEngine.decideQuery(request, queryConfig).mode();
        if (mode == QueryMode.MODEL_ONLY) {
            return QueryStrategy.SINGLE;
        }
        if (mode == QueryMode.CACHE_ONLY) {
            return QueryStrategy.CACHED;
        }
        var facets = classification.facets();
        if (facets.isEmpty()) {
            return QueryStrategy.SINGLE;
        }
        var local = Set.of(IntentFacet.DOCUMENTO, IntentFacet.RAG);
        var external = facets.stream().filter(facet -> !local.contains(facet)).toList();
        if (external.isEmpty()) {
            return QueryStrategy.LOCAL_ONLY;
        }
        if (facets.size() == 1) {
            return facets.contains(IntentFacet.TENDENCIAS)
                ? QueryStrategy.INTERNET_ONLY
                : QueryStrategy.SINGLE;
        }
        if (external.size() < facets.size()) {
            return QueryStrategy.HYBRID;
        }
        if (classification.domains().size() > 1) {
            return QueryStrategy.HYBRID;
        }
        if (facets.size() == 2) {
            if (facets.contains(IntentFacet.REGULATORIA) && facets.contains(IntentFacet.LEGAL)) {
                return QueryStrategy.SEQUENTIAL;
            }
            return QueryStrategy.MULTI;
        }
        return QueryStrategy.PARALLEL;
    }
}
