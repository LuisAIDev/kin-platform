package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.engine.EngineTestFixtures;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryEnterprisePipelineResultStoreTest {

    private final InMemoryEnterprisePipelineResultStore store = new InMemoryEnterprisePipelineResultStore();

    @Test
    void storeYConsume_devuelvenLosMismosResultados() {
        var projectId = UUID.randomUUID();
        var results = new EnterpriseTurnResults(projectId,
            EngineTestFixtures.recommendations(0.8), EngineTestFixtures.opportunities(0.8),
            EngineTestFixtures.knowledge(0.8), EngineTestFixtures.riskResult(0.8));

        store.store(results);

        var consumed = store.consume(projectId);
        assertTrue(consumed.isPresent());
        assertEquals(results, consumed.get());
    }

    @Test
    void consumeEliminaLosResultados_consumoUnico() {
        var projectId = UUID.randomUUID();
        store.store(new EnterpriseTurnResults(projectId,
            EngineTestFixtures.recommendations(0.8), null, null, null));

        store.consume(projectId);

        assertTrue(store.consume(projectId).isEmpty());
    }

    @Test
    void consumeSinResultados_devuelveVacio() {
        assertTrue(store.consume(UUID.randomUUID()).isEmpty());
    }

    @Test
    void consumeConProjectIdNulo_devuelveVacio() {
        assertTrue(store.consume(null).isEmpty());
    }

    @Test
    void storeConResultsNulo_oProjectIdNulo_noOpera() {
        store.store(null);
        store.store(new EnterpriseTurnResults(null, null, null, null, null));

        assertTrue(store.consume(UUID.randomUUID()).isEmpty());
    }

    @Test
    void storeSobreescribeLaEntradaDelProyecto() {
        var projectId = UUID.randomUUID();
        var first = new EnterpriseTurnResults(projectId,
            EngineTestFixtures.recommendations(0.5), null, null, null);
        var second = new EnterpriseTurnResults(projectId,
            EngineTestFixtures.recommendations(0.9), null, null, null);

        store.store(first);
        store.store(second);

        var consumed = store.consume(projectId);
        assertTrue(consumed.isPresent());
        assertFalse(consumed.get().recommendations().isEmpty());
        assertEquals(0.9, consumed.get().recommendations().confidence());
    }
}
