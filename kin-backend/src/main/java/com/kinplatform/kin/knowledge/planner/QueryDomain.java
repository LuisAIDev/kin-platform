package com.kinplatform.kin.knowledge.planner;

/**
 * Dominio de conocimiento objetivo de una consulta (especificación Fase 3).
 * Se deriva determinísticamente de las facetas detectadas y se usa para
 * decidir estrategias (p. ej. varios dominios distintos = estrategia híbrida).
 */
public enum QueryDomain {

    LEGAL("Legal"),
    MARKET("Mercado"),
    STATISTICAL("Estadístico"),
    TECHNICAL("Técnico"),
    ACADEMIC("Académico"),
    DOCUMENTAL("Documental"),
    STABLE("Estable"),
    GENERAL("General");

    private final String displayName;

    QueryDomain(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
