package com.kinplatform.kin.knowledge.orchestrator;

/**
 * Tipos de decisión emitidos por el Knowledge Orchestrator (especificación
 * Fase 5). El orquestador consulta únicamente al Knowledge Policy Engine para
 * decidir; estas son las decisiones que registra en el ciclo.
 */
public enum OrchestrationDecisionType {

    CONSULT_EXTERNAL("Consultar fuentes externas"),
    NO_CONSULT("No consultar"),
    CACHE_ONLY("Solo caché"),
    STOP("Detener"),
    STOP_CONSULTS("Detener consultas"),
    DEGRADE("Degradar"),
    FETCH_COORDINATED("Adquisición coordinada"),
    VALIDATION_OK("Validación delegada"),
    RANKING_OK("Ranking delegado"),
    ASSEMBLY_OK("Ensamblado delegado"),
    FINALIZE("Finalizar"),
    FAIL("Fallar");

    private final String displayName;

    OrchestrationDecisionType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
