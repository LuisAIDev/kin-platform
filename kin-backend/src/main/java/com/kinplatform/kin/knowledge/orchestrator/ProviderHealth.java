package com.kinplatform.kin.knowledge.orchestrator;

/**
 * Salud de un proveedor en el entorno de ejecución (especificación Fase 5):
 * valor de dominio que modela disponibilidad sin tocar infraestructura. Un
 * proveedor {@code DOWN} o {@code TIMEOUT} dispara degradación o fallo según la
 * estrategia.
 */
public enum ProviderHealth {

    AVAILABLE("Disponible"),
    DOWN("Caído"),
    TIMEOUT("Timeout");

    private final String displayName;

    ProviderHealth(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
