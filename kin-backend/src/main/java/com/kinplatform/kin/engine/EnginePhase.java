package com.kinplatform.kin.engine;

/**
 * Fase del pipeline en la que se ejecuta un engine.
 *
 * <p>Permite agrupar motores por fase y, en el futuro, ejecutar las fases de
 * forma secuencial, condicional o por prioridades sin acoplar los engines
 * entre sí.</p>
 */
public enum EnginePhase {
    ANALYSIS("Análisis"),
    EVALUATION("Evaluación"),
    STRATEGY("Estrategia"),
    CONSULTATION("Consultoría"),
    SCORING("Scoring"),
    RECOMMENDATION("Recomendaciones"),
    RISK("Riesgos"),
    OPPORTUNITY("Oportunidades"),
    KNOWLEDGE("Conocimiento"),
    INNOVATION("Innovación"),
    COMPETITION("Competencia"),
    FINANCIAL("Financiero"),
    MARKET("Mercado"),
    VALIDATION("Validación"),
    REPORTING("Reporte"),
    EXPLANATION("Explicación");

    private final String displayName;

    EnginePhase(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
