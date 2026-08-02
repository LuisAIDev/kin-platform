package com.kinplatform.kin.enrichment;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia del resultado del enriquecimiento (ADR-016).
 *
 * <p>Definido en el dominio; la infraestructura lo implementará en una etapa
 * posterior (Etapa E4) con un almacén propio, sin tocar {@code project_context}.
 * {@code findOrEmpty} es un default determinista sobre {@code find}: devuelve
 * el resultado persistido o un {@link EnrichmentResult} vacío para el proyecto
 * (offline-first).</p>
 */
public interface EnrichmentRepository {

    Optional<EnrichmentResult> find(UUID projectId);

    void save(UUID projectId, EnrichmentResult result);

    default EnrichmentResult findOrEmpty(UUID projectId) {
        return find(projectId).orElseGet(EnrichmentResult::empty);
    }
}
