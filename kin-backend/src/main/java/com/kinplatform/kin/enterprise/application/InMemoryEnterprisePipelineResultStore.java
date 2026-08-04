package com.kinplatform.kin.enterprise.application;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación en memoria de {@link EnterprisePipelineResultStore} (Fase 10,
 * Milestone 3C).
 *
 * <p>Correlaciona los resultados del último turno {@code REPORT} por
 * {@code projectId}: {@link #store} sobreescribe la entrada del proyecto y
 * {@link #consume} la recupera y la elimina (consumo único). Thread-safe
 * (mapa concurrente); adecuado para despliegue de una sola JVM, donde la
 * captura del runtime y el consumo del listener ocurren en el mismo nodo.</p>
 */
public final class InMemoryEnterprisePipelineResultStore implements EnterprisePipelineResultStore {

    private final Map<UUID, EnterpriseTurnResults> results = new ConcurrentHashMap<>();

    @Override
    public void store(EnterpriseTurnResults results) {
        if (results != null && results.projectId() != null) {
            this.results.put(results.projectId(), results);
        }
    }

    @Override
    public Optional<EnterpriseTurnResults> consume(UUID projectId) {
        if (projectId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(results.remove(projectId));
    }
}
