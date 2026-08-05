package com.kinplatform.kin.knowledge.planner;

/**
 * Estrategia de ejecución del plan (especificación Fase 3). El Query Planner
 * decide la estrategia con reglas deterministas; el orquestador (Fase 5) la
 * ejecuta. Ninguna estrategia ejecuta consultas por sí misma.
 */
public enum QueryStrategy {

    SINGLE("Consulta única"),
    MULTI("Múltiples consultas"),
    PARALLEL("Paralelo"),
    SEQUENTIAL("Secuencial"),
    CACHED("Caché"),
    LOCAL_ONLY("Solo local"),
    INTERNET_ONLY("Solo internet"),
    HYBRID("Híbrido");

    private final String displayName;

    QueryStrategy(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
