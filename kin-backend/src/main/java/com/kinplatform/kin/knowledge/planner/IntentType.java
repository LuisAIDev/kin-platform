package com.kinplatform.kin.knowledge.planner;

/**
 * Tipo de intención primaria detectada por el Query Planner (especificación
 * Fase 3). Valor de dominio inmutable: la clasificación es 100 % determinista
 * (reglas declarativas, sin IA).
 */
public enum IntentType {

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
    RAG("RAG"),
    CONOCIMIENTO_ESTABLE("Conocimiento estable"),
    GENERAL("General");

    private final String displayName;

    IntentType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
