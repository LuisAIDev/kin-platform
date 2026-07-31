package com.kinplatform.kin.reporting;

/**
 * Categorías de recomendaciones que puede emitir el RecommendationEngine.
 */
public enum RecommendationCategory {
    VALIDATION("Validaci\u00F3n"),
    MARKETING("Marketing"),
    FINANCIAL("Financiero"),
    PRODUCT("Producto"),
    STRATEGY("Estrategia"),
    OPERATIONS("Operaciones"),
    INNOVATION("Innovaci\u00F3n"),
    TEAM("Equipo");

    private final String displayName;

    RecommendationCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
