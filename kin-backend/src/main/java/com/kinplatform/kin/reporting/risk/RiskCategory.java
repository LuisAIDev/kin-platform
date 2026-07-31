package com.kinplatform.kin.reporting.risk;

/**
 * Categorías de riesgo analizadas por los RiskAnalyzers.
 * Cada analizador está especializado en una única categoría.
 */
public enum RiskCategory {
    BUSINESS("Negocio"),
    TECHNICAL("Técnico"),
    FINANCIAL("Financiero"),
    MARKET("Mercado");

    private final String displayName;

    RiskCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
