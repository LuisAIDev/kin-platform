package com.kinplatform.kin.knowledge.policy;

import java.util.List;

/**
 * Resultado inmutable de las políticas de consulta (especificación Fase 2):
 * modo de adquisición decidido y motivos que lo sustentan.
 */
public record QueryPolicyResult(
    QueryMode mode,
    List<String> reasons
) {

    public QueryPolicyResult {
        mode = mode == null ? QueryMode.EXTERNAL : mode;
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public boolean consultExternal() {
        return mode == QueryMode.EXTERNAL;
    }

    public boolean modelOnly() {
        return mode == QueryMode.MODEL_ONLY;
    }

    public boolean cacheOnly() {
        return mode == QueryMode.CACHE_ONLY;
    }

    public static QueryPolicyResult of(QueryMode mode, String reason) {
        return new QueryPolicyResult(mode, List.of(reason == null ? "" : reason));
    }
}
