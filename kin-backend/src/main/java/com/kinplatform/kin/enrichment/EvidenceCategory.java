package com.kinplatform.kin.enrichment;

/**
 * Categoría de análisis objetivo del enriquecimiento con conocimiento externo
 * (ADR-016). Un hecho verificado se asocia a una o más categorías según su
 * relevancia semántica con el proyecto.
 *
 * <p>Valor del dominio: la selección de categorías y su umbral son decisiones
 * de Java; nunca se le pregunta al LLM a qué categoría pertenece un hecho.</p>
 */
public enum EvidenceCategory {

    MARKET("Mercado"),
    INNOVATION("Innovación"),
    FINANCIAL("Financiero"),
    COMPETITIVE("Competitivo");

    private final String displayName;

    EvidenceCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
