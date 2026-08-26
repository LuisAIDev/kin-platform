package com.kinplatform.kin.health.triage.adapter;

import com.kinplatform.kin.health.triage.domain.CatalogUpdate;
import com.kinplatform.kin.health.triage.domain.CatalogUpdateResult;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import com.kinplatform.kin.health.triage.port.TriageKnowledgeRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

/**
 * Decorador de caché para el catálogo de triaje (fase de producción).
 *
 * <p>Envuelve el {@link TriageKnowledgeRepository} para cachear el catálogo
 * (condiciones/síntomas/relaciones) con {@code @Cacheable} (Spring Cache). El
 * caché se invalida automáticamente en {@code applyUpdate} (importación o
 * actualización). La caché por defecto es en memoria ({@code simple}); si se
 * habilita {@code spring.cache.type=redis} + {@code kin.cache.redis.enabled=true},
 * usa Redis. Offline-safe: si la caché falla, el método delega al repositorio.</p>
 */
public class CachedTriageKnowledgeRepository implements TriageKnowledgeRepository {

    private static final String CACHE_NAME = "kin.triage.catalog";

    private final TriageKnowledgeRepository delegate;

    public CachedTriageKnowledgeRepository(TriageKnowledgeRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    @Cacheable(cacheNames = CACHE_NAME, key = "'catalog'")
    public TriageCatalog loadCatalog() {
        return delegate.loadCatalog();
    }

    @Override
    @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
    public CatalogUpdateResult applyUpdate(CatalogUpdate update) {
        return delegate.applyUpdate(update);
    }
}
