package com.kinplatform.kin.reporting.opportunity;

/**
 * Categorías de oportunidad analizadas por los OpportunityAnalyzers.
 * Cada analizador está especializado en una única categoría.
 */
public enum OpportunityCategory {
    MERCADO("Mercado"),
    INNOVACION("Innovación"),
    TECNOLOGICA("Tecnológica"),
    FINANCIERA("Financiera"),
    COMPETITIVA("Competitiva"),
    ESCALABILIDAD("Escalabilidad"),
    AUTOMATIZACION("Automatización"),
    MONETIZACION("Monetización");

    private final String displayName;

    OpportunityCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
