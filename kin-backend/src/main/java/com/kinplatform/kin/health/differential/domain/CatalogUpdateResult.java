package com.kinplatform.kin.health.differential.domain;

import java.time.OffsetDateTime;

/**
 * Resultado de una actualización del catálogo diferencial (ADR-029).
 */
public record CatalogUpdateResult(int riskFactorsAdded, int testsAdded, String source, OffsetDateTime appliedAt) {

    public CatalogUpdateResult {
        source = source == null ? "" : source;
        appliedAt = appliedAt == null ? OffsetDateTime.now() : appliedAt;
    }

    public boolean changed() {
        return riskFactorsAdded > 0 || testsAdded > 0;
    }

    public static CatalogUpdateResult empty(String source) {
        return new CatalogUpdateResult(0, 0, source, OffsetDateTime.now());
    }

    public static CatalogUpdateResult of(int riskFactorsAdded, int testsAdded, String source) {
        return new CatalogUpdateResult(riskFactorsAdded, testsAdded, source, OffsetDateTime.now());
    }
}
