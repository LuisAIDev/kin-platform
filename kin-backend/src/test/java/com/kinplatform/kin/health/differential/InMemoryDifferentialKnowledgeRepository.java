package com.kinplatform.kin.health.differential;

import com.kinplatform.kin.health.differential.domain.DifferentialCatalog;
import com.kinplatform.kin.health.differential.port.DifferentialKnowledgeRepository;

/**
 * Implementación en memoria del puerto {@link DifferentialKnowledgeRepository}
 * para tests (ADR-029).
 */
public class InMemoryDifferentialKnowledgeRepository implements DifferentialKnowledgeRepository {

    private DifferentialCatalog catalog;

    public InMemoryDifferentialKnowledgeRepository(DifferentialCatalog catalog) {
        this.catalog = catalog == null ? DifferentialCatalog.empty() : catalog;
    }

    @Override
    public DifferentialCatalog loadCatalog() {
        return catalog;
    }

    @Override
    public com.kinplatform.kin.health.differential.domain.CatalogUpdateResult applyUpdate(
            com.kinplatform.kin.health.differential.domain.CatalogUpdate update) {
        return com.kinplatform.kin.health.differential.domain.CatalogUpdateResult.empty(
                update == null ? "" : update.source());
    }
}
