package com.kinplatform.kin.enterprise.aggregate;

import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseProjectTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    static DocumentArtifact document(DocumentType type) {
        return new DocumentArtifact(UUID.randomUUID(), type, "contenido de " + type,
            OffsetDateTime.now(), "BusinessModelEngine", "1.0.0", "hash-" + type, 1);
    }

    // ------------------------------------------------------------------
    // Fábricas estáticas
    // ------------------------------------------------------------------

    @Test
    void request_deberiaCrearProyectoEnEstadoRequested() {
        var project = EnterpriseProject.request(PROJECT_ID, 1);

        assertEquals(PROJECT_ID, project.projectId());
        assertEquals(1, project.version());
        assertEquals(GenerationStatus.REQUESTED, project.status());
        assertNotNull(project.createdAt());
        assertNotNull(project.updatedAt());
        assertNull(project.completedAt());
        assertNull(project.failedReason());
        assertTrue(project.documents().isEmpty());
        assertTrue(project.isRequested());
        assertFalse(project.isRunning());
        assertFalse(project.isCompleted());
        assertFalse(project.isFailed());
        assertFalse(project.canRegenerate());
        assertEquals(0, project.documentCount());
    }

    @Test
    void request_conVersionCero_deberiaLanzar() {
        assertThrows(EnterpriseProjectException.class, () -> EnterpriseProject.request(PROJECT_ID, 0));
    }

    @Test
    void request_conVersionNegativa_deberiaLanzar() {
        assertThrows(EnterpriseProjectException.class, () -> EnterpriseProject.request(PROJECT_ID, -1));
    }

    @Test
    void request_conProjectIdNulo_deberiaLanzar() {
        assertThrows(EnterpriseProjectException.class, () -> EnterpriseProject.request(null, 1));
    }

    @Test
    void start_deberiaReconstruirProyectoEnEstadoRunning() {
        var docs = List.of(document(DocumentType.LEAN_CANVAS));
        var now = OffsetDateTime.now();

        var project = EnterpriseProject.start(PROJECT_ID, 1, now, now.plusMinutes(1), docs);

        assertEquals(GenerationStatus.RUNNING, project.status());
        assertTrue(project.isRunning());
        assertNull(project.completedAt());
        assertNull(project.failedReason());
        assertEquals(docs, project.documents());
        assertEquals(1, project.documentCount());
    }

    @Test
    void complete_deberiaReconstruirProyectoEnEstadoCompleted() {
        var docs = List.of(document(DocumentType.LEAN_CANVAS));
        var now = OffsetDateTime.now();
        var completedAt = now.plusMinutes(5);

        var project = EnterpriseProject.complete(PROJECT_ID, 1, now, completedAt, completedAt, docs);

        assertEquals(GenerationStatus.COMPLETED, project.status());
        assertTrue(project.isCompleted());
        assertTrue(project.canRegenerate());
        assertEquals(completedAt, project.completedAt());
        assertNull(project.failedReason());
        assertEquals(docs, project.documents());
    }

    @Test
    void complete_sinCompletedAt_deberiaLanzar() {
        var now = OffsetDateTime.now();
        assertThrows(EnterpriseProjectException.class,
            () -> EnterpriseProject.complete(PROJECT_ID, 1, now, now, null, List.of()));
    }

    @Test
    void fail_deberiaReconstruirProyectoEnEstadoFailed() {
        var docs = List.of(document(DocumentType.LEAN_CANVAS));
        var now = OffsetDateTime.now();

        var project = EnterpriseProject.fail(PROJECT_ID, 1, now, now.plusMinutes(1), "LLM no disponible", docs);

        assertEquals(GenerationStatus.FAILED, project.status());
        assertTrue(project.isFailed());
        assertTrue(project.canRegenerate());
        assertEquals("LLM no disponible", project.failedReason());
        assertNull(project.completedAt());
        assertEquals(docs, project.documents());
    }

    @Test
    void fail_sinMotivo_deberiaLanzar() {
        var now = OffsetDateTime.now();
        assertThrows(EnterpriseProjectException.class,
            () -> EnterpriseProject.fail(PROJECT_ID, 1, now, now, null, List.of()));
    }

    @Test
    void fail_conMotivoEnBlanco_deberiaLanzar() {
        var now = OffsetDateTime.now();
        assertThrows(EnterpriseProjectException.class,
            () -> EnterpriseProject.fail(PROJECT_ID, 1, now, now, "   ", List.of()));
    }

    // ------------------------------------------------------------------
    // Invariantes del constructor
    // ------------------------------------------------------------------

    @Test
    void start_conCreatedAtNulo_deberiaLanzar() {
        var now = OffsetDateTime.now();
        assertThrows(EnterpriseProjectException.class,
            () -> EnterpriseProject.start(PROJECT_ID, 1, null, now, List.of()));
    }

    @Test
    void start_conUpdatedAtNulo_deberiaLanzar() {
        var now = OffsetDateTime.now();
        assertThrows(EnterpriseProjectException.class,
            () -> EnterpriseProject.start(PROJECT_ID, 1, now, null, List.of()));
    }

    @Test
    void start_conUpdatedAtAnteriorAlCreatedAt_deberiaLanzar() {
        var now = OffsetDateTime.now();
        assertThrows(EnterpriseProjectException.class,
            () -> EnterpriseProject.start(PROJECT_ID, 1, now, now.minusMinutes(1), List.of()));
    }

    @Test
    void start_conDocumentsNulos_deberiaLanzar() {
        var now = OffsetDateTime.now();
        assertThrows(EnterpriseProjectException.class,
            () -> EnterpriseProject.start(PROJECT_ID, 1, now, now, null));
    }

    @Test
    void start_conDocumentoNuloEnLaLista_deberiaLanzar() {
        var now = OffsetDateTime.now();
        var docs = new ArrayList<DocumentArtifact>();
        docs.add(document(DocumentType.LEAN_CANVAS));
        docs.add(null);
        assertThrows(EnterpriseProjectException.class,
            () -> EnterpriseProject.start(PROJECT_ID, 1, now, now, docs));
    }

    @Test
    void start_conTiposDuplicados_deberiaLanzar() {
        var now = OffsetDateTime.now();
        var docs = List.of(document(DocumentType.LEAN_CANVAS), document(DocumentType.LEAN_CANVAS));
        assertThrows(EnterpriseProjectException.class,
            () -> EnterpriseProject.start(PROJECT_ID, 1, now, now, docs));
    }

    // ------------------------------------------------------------------
    // Máquina de estados
    // ------------------------------------------------------------------

    @Test
    void cicloCompleto_deberiaRecorrerRequestedRunningCompleted() {
        var requested = EnterpriseProject.request(PROJECT_ID, 1);
        var completed = requested.startGeneration().completeGeneration();

        assertEquals(GenerationStatus.COMPLETED, completed.status());
        assertTrue(completed.isCompleted());
        assertNotNull(completed.completedAt());
        assertEquals(requested.createdAt(), completed.createdAt());
        assertTrue(!completed.updatedAt().isBefore(requested.updatedAt()));
    }

    @Test
    void cicloFallido_deberiaRecorrerRequestedRunningFailed() {
        var failed = EnterpriseProject.request(PROJECT_ID, 1).startGeneration()
            .failGeneration("Fallo del proveedor");

        assertEquals(GenerationStatus.FAILED, failed.status());
        assertTrue(failed.isFailed());
        assertEquals("Fallo del proveedor", failed.failedReason());
        assertNull(failed.completedAt());
    }

    @Test
    void startGeneration_deberiaPreservarCreatedAt_yRenovarUpdatedAt() {
        var requested = EnterpriseProject.request(PROJECT_ID, 1);
        var running = requested.startGeneration();

        assertEquals(requested.createdAt(), running.createdAt());
        assertTrue(!running.updatedAt().isBefore(requested.updatedAt()));
        assertNull(running.completedAt());
    }

    @Test
    void startGeneration_desdeRunning_deberiaLanzar() {
        var running = EnterpriseProject.request(PROJECT_ID, 1).startGeneration();
        assertThrows(EnterpriseProjectException.class, running::startGeneration);
    }

    @Test
    void startGeneration_desdeCompleted_deberiaLanzar() {
        var completed = EnterpriseProject.request(PROJECT_ID, 1).startGeneration().completeGeneration();
        assertThrows(EnterpriseProjectException.class, completed::startGeneration);
    }

    @Test
    void startGeneration_desdeFailed_deberiaLanzar() {
        var failed = EnterpriseProject.request(PROJECT_ID, 1).startGeneration().failGeneration("fallo");
        assertThrows(EnterpriseProjectException.class, failed::startGeneration);
    }

    @Test
    void completeGeneration_desdeRequested_deberiaLanzar() {
        var requested = EnterpriseProject.request(PROJECT_ID, 1);
        assertThrows(EnterpriseProjectException.class, requested::completeGeneration);
    }

    @Test
    void completeGeneration_desdeCompleted_deberiaLanzar() {
        var completed = EnterpriseProject.request(PROJECT_ID, 1).startGeneration().completeGeneration();
        assertThrows(EnterpriseProjectException.class, completed::completeGeneration);
    }

    @Test
    void completeGeneration_desdeFailed_deberiaLanzar() {
        var failed = EnterpriseProject.request(PROJECT_ID, 1).startGeneration().failGeneration("fallo");
        assertThrows(EnterpriseProjectException.class, failed::completeGeneration);
    }

    @Test
    void failGeneration_desdeRequested_deberiaLanzar() {
        var requested = EnterpriseProject.request(PROJECT_ID, 1);
        assertThrows(EnterpriseProjectException.class, () -> requested.failGeneration("fallo"));
    }

    @Test
    void failGeneration_desdeCompleted_deberiaLanzar() {
        var completed = EnterpriseProject.request(PROJECT_ID, 1).startGeneration().completeGeneration();
        assertThrows(EnterpriseProjectException.class, () -> completed.failGeneration("fallo"));
    }

    @Test
    void failGeneration_desdeFailed_deberiaLanzar() {
        var failed = EnterpriseProject.request(PROJECT_ID, 1).startGeneration().failGeneration("fallo");
        assertThrows(EnterpriseProjectException.class, () -> failed.failGeneration("otro fallo"));
    }

    @Test
    void failGeneration_conMotivoEnBlanco_deberiaLanzar() {
        var running = EnterpriseProject.request(PROJECT_ID, 1).startGeneration();
        assertThrows(EnterpriseProjectException.class, () -> running.failGeneration("  "));
    }

    @Test
    void completeGeneration_deberiaConservarLosDocumentosGenerados() {
        var completed = EnterpriseProject.request(PROJECT_ID, 1)
            .startGeneration()
            .attachDocument(document(DocumentType.LEAN_CANVAS))
            .attachDocument(document(DocumentType.FINANCIAL_PLAN))
            .completeGeneration();

        assertTrue(completed.isCompleted());
        assertEquals(2, completed.documentCount());
        assertTrue(completed.hasDocument(DocumentType.LEAN_CANVAS));
        assertTrue(completed.hasDocument(DocumentType.FINANCIAL_PLAN));
    }

    @Test
    void isRequested_deberiaSerFalsoEnEstadosAvanzados() {
        var running = EnterpriseProject.request(PROJECT_ID, 1).startGeneration();
        var completed = running.completeGeneration();
        var failed = EnterpriseProject.request(PROJECT_ID, 1).startGeneration().failGeneration("fallo");

        assertFalse(running.isRequested());
        assertFalse(completed.isRequested());
        assertFalse(failed.isRequested());
        assertFalse(running.isCompleted());
        assertFalse(failed.isCompleted());
    }
}
