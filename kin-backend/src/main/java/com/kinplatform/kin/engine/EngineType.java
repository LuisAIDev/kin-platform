package com.kinplatform.kin.engine;

/**
 * Tipo de motor según la naturaleza de su evaluación.
 */
public enum EngineType {
    DOMAIN("Motor de dominio puro"),
    ADAPTER("Adaptador sobre un motor existente");

    private final String description;

    EngineType(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
