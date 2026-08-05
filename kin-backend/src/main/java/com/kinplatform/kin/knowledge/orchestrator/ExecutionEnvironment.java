package com.kinplatform.kin.knowledge.orchestrator;

import com.kinplatform.kin.knowledge.planner.ProviderType;

import java.util.Map;

/**
 * Entorno de ejecución declarativo (especificación Fase 5): disponibilidad de
 * Internet, salud de la caché y salud de cada tipo de proveedor. Valor de
 * dominio inmutable que la capa de aplicación proveerá en la integración; en
 * pruebas se declara directamente. Permite modelar "proveedor caído", "timeout",
 * "sin Internet" y "caché corrupta" sin infraestructura.
 */
public record ExecutionEnvironment(
    boolean internetAvailable,
    boolean cacheHealthy,
    Map<ProviderType, ProviderHealth> providerHealth
) {

    public ExecutionEnvironment {
        providerHealth = providerHealth == null ? Map.of() : Map.copyOf(providerHealth);
    }

    public static ExecutionEnvironment online() {
        return new ExecutionEnvironment(true, true, Map.of());
    }

    public static ExecutionEnvironment offline() {
        return new ExecutionEnvironment(false, false, Map.of());
    }

    public ProviderHealth healthOf(ProviderType type) {
        if (type == null) {
            return ProviderHealth.AVAILABLE;
        }
        return providerHealth.getOrDefault(type, ProviderHealth.AVAILABLE);
    }

    public boolean isAvailable(ProviderType type) {
        return healthOf(type) == ProviderHealth.AVAILABLE;
    }
}
