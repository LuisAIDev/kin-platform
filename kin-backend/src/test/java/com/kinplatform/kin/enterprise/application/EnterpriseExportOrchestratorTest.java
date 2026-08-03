package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseExportOrchestratorTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    private InMemoryEnterpriseProjectRepository repository() {
        return new InMemoryEnterpriseProjectRepository();
    }

    @Test
    void export_deberiaRecuperarLaVersionYExportarla() {
        var repository = repository();
        repository.save(ExportTestFixtures.project(PROJECT_ID, 1,
            DocumentType.LEAN_CANVAS, DocumentType.KPI));
        var orchestrator = new EnterpriseExportOrchestrator(repository);

        var bundle = orchestrator.export(PROJECT_ID, 1);

        assertEquals(PROJECT_ID, bundle.projectId());
        assertEquals(1, bundle.version());
        assertEquals(2, bundle.documentCount());
        assertEquals(6, bundle.renderingCount());
    }

    @Test
    void export_deberiaExportarLaVersionSolicitadaEntreVarias() {
        var repository = repository();
        repository.save(ExportTestFixtures.project(PROJECT_ID, 1, DocumentType.LEAN_CANVAS));
        repository.save(ExportTestFixtures.project(PROJECT_ID, 2, DocumentType.KPI));
        var orchestrator = new EnterpriseExportOrchestrator(repository);

        var bundle = orchestrator.export(PROJECT_ID, 1);

        assertEquals(1, bundle.version());
        assertTrue(bundle.types().contains(DocumentType.LEAN_CANVAS));
    }

    @Test
    void exportLatest_deberiaExportarLaUltimaVersion() {
        var repository = repository();
        repository.save(ExportTestFixtures.project(PROJECT_ID, 1, DocumentType.LEAN_CANVAS));
        repository.save(ExportTestFixtures.project(PROJECT_ID, 2, DocumentType.KPI));
        var orchestrator = new EnterpriseExportOrchestrator(repository);

        var bundle = orchestrator.exportLatest(PROJECT_ID);

        assertEquals(2, bundle.version());
        assertTrue(bundle.types().contains(DocumentType.KPI));
    }

    @Test
    void export_conVersionInexistente_deberiaLanzarExcepcionDeDominio() {
        var orchestrator = new EnterpriseExportOrchestrator(repository());

        assertThrows(EnterpriseExportException.class, () -> orchestrator.export(PROJECT_ID, 1));
        assertThrows(EnterpriseExportException.class, () -> orchestrator.export(PROJECT_ID, 99));
    }

    @Test
    void exportLatest_sinVersiones_deberiaLanzarExcepcionDeDominio() {
        var orchestrator = new EnterpriseExportOrchestrator(repository());

        assertThrows(EnterpriseExportException.class, () -> orchestrator.exportLatest(PROJECT_ID));
    }

    @Test
    void exportIfPresent_deberiaDevolverLaVersionSiExiste() {
        var repository = repository();
        repository.save(ExportTestFixtures.project(PROJECT_ID, 1, DocumentType.ROADMAP));
        var orchestrator = new EnterpriseExportOrchestrator(repository);

        var bundle = orchestrator.exportIfPresent(PROJECT_ID, 1);
        assertTrue(bundle.isPresent());
        assertEquals(1, bundle.get().version());

        assertTrue(orchestrator.exportIfPresent(PROJECT_ID, 5).isEmpty());
    }

    @Test
    void constructorPorDefecto_deberiaCablearLosRenderizadores() {
        var repository = repository();
        repository.save(ExportTestFixtures.project(PROJECT_ID, 1, DocumentType.MARKET_PLAN));
        var orchestrator = new EnterpriseExportOrchestrator(repository);

        var bundle = orchestrator.export(PROJECT_ID, 1);

        assertEquals(3, bundle.renderingsFor(DocumentType.MARKET_PLAN).size());
    }

    @Test
    void constructorConDependenciaNula_deberiaLanzar() {
        var service = new EnterpriseExportService(new EnterpriseRendererFactory());
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseExportOrchestrator(null));
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseExportOrchestrator(repository(), null));
    }

    @Test
    void export_conProjectIdNulo_deberiaLanzarExcepcionDeDominio() {
        var orchestrator = new EnterpriseExportOrchestrator(repository());
        assertThrows(EnterpriseExportException.class, () -> orchestrator.export(null, 1));
    }

    @Test
    void export_deUnaVersionSinDocumentos_deberiaDevolverBundleVacio() {
        var repository = repository();
        var now = java.time.OffsetDateTime.now();
        repository.save(EnterpriseProject.complete(PROJECT_ID, 1, now, now, now, java.util.List.of()));
        var orchestrator = new EnterpriseExportOrchestrator(repository);

        var bundle = orchestrator.export(PROJECT_ID, 1);

        assertEquals(0, bundle.documentCount());
        assertEquals(0, bundle.renderingCount());
    }
}
