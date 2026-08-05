package com.kinplatform.kin.knowledge.planner;

/**
 * Faceta individual de intención detectada por el Query Planner (especificación
 * Fase 3). Una misma consulta puede tener múltiples facetas (p. ej. panadería:
 * regulatoria, mercado, estadística, competencia).
 */
public enum IntentFacet {

    REGULATORIA("Regulatoria"),
    LEGAL("Legal"),
    FINANCIERA("Financiera"),
    ACADEMICA("Académica"),
    TECNICA("Técnica"),
    MERCADO("Mercado"),
    ESTADISTICA("Estadística"),
    COMPETENCIA("Competencia"),
    TENDENCIAS("Tendencias"),
    DOCUMENTO("Documento"),
    RAG("RAG");

    private final String displayName;

    IntentFacet(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
