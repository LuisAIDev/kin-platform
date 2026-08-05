package com.kinplatform.kin.knowledge.policy;

/**
 * Categoría oficial de políticas del Knowledge Policy Engine (especificación
 * Fase 2): agrupa las reglas por etapa del ciclo de adquisición de
 * conocimiento. Valor de dominio: nunca depende de Spring ni de infraestructura.
 */
public enum PolicyCategory {

    QUERY("Consulta"),
    PROVIDER("Proveedores"),
    QUALITY("Calidad"),
    COST("Costo"),
    CONTEXT("Contexto");

    private final String displayName;

    PolicyCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
