package com.kinplatform.kin.knowledge.policy;

/**
 * Modo de adquisición decidido por las políticas de consulta (especificación
 * Fase 2): responder únicamente con el conocimiento del modelo, usar solo la
 * caché o consultar fuentes externas.
 */
public enum QueryMode {

    MODEL_ONLY("Solo modelo"),
    CACHE_ONLY("Solo caché"),
    EXTERNAL("Consulta externa");

    private final String displayName;

    QueryMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
