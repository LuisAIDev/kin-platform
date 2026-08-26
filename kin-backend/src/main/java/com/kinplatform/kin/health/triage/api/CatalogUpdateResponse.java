package com.kinplatform.kin.health.triage.api;

import com.kinplatform.kin.health.triage.domain.CatalogUpdateResult;
import java.time.OffsetDateTime;

/**
 * Respuesta del endpoint administrativo de actualización del catálogo
 * (ADR-028, fase profesional).
 */
public record CatalogUpdateResponse(
        int symptomsAdded,
        int conditionsAdded,
        int relationsAdded,
        String source,
        OffsetDateTime appliedAt,
        boolean changed) {

    public static CatalogUpdateResponse from(CatalogUpdateResult result) {
        return new CatalogUpdateResponse(
                result.symptomsAdded(),
                result.conditionsAdded(),
                result.relationsAdded(),
                result.source(),
                result.appliedAt(),
                result.changed());
    }
}
