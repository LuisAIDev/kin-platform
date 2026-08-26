package com.kinplatform.kin.health.triage.domain;

import java.time.OffsetDateTime;

/**
 * Resultado de una actualización del catálogo de triaje (ADR-028, fase
 * profesional): cuenta de elementos añadidos/actualizados por tipo y la fuente.
 */
public record CatalogUpdateResult(
        int symptomsAdded, int conditionsAdded, int relationsAdded, String source, OffsetDateTime appliedAt) {

    public CatalogUpdateResult {
        source = source == null ? "" : source;
        appliedAt = appliedAt == null ? OffsetDateTime.now() : appliedAt;
    }

    public boolean changed() {
        return symptomsAdded > 0 || conditionsAdded > 0 || relationsAdded > 0;
    }

    public static CatalogUpdateResult empty(String source) {
        return new CatalogUpdateResult(0, 0, 0, source, OffsetDateTime.now());
    }

    public static CatalogUpdateResult of(int symptomsAdded, int conditionsAdded, int relationsAdded, String source) {
        return new CatalogUpdateResult(symptomsAdded, conditionsAdded, relationsAdded, source, OffsetDateTime.now());
    }
}
