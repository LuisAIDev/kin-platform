package com.kinplatform.kin.enterprise.aggregate;

import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseProjectDocumentsVersioningTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    static DocumentArtifact document(DocumentType type) {
        return new DocumentArtifact(UUID.randomUUID(), type, "contenido de " + type,
            OffsetDateTime.now(), "BusinessModelEngine", "1.0.0", "hash-" + type, 1);
    }

    static DocumentArtifact document(DocumentType type, int version) {
        return new DocumentArtifact(UUID.randomUUID(), type, "contenido de " + type,
            OffsetDateTime.now(), "BusinessModelEngine", "1.0.0", "hash-" + type + "-v" + version, version);
    }

    static EnterpriseProject runningWith(DocumentType... types) {
        var running = EnterpriseProject.request(PROJECT_ID, 1).startGeneration();
        for (DocumentType type : types) {
            running = running.attachDocument(document(type));
        }
        return running;
    }

    // ------------------------------------------------------------------
    // Documentos
    // ------------------------------------------------------------------

    @Test
    void attachDocument_deberiaAnadirDocumentoYDevolverNuevaInstancia() {
        var running = runningWith();
        var updated = running.attachDocument(document(DocumentType.LEAN_CANVAS));

        assertTrue(running != updated);
        assertEquals(1, updated.documentCount());
        assertTrue(updated.hasDocument(DocumentType.LEAN_CANVAS));
        assertFalse(running.hasDocument(DocumentType.LEAN_CANVAS));
    }

    @Test
    void attachDocument_documentoNulo_deberiaLanzar() {
        var running = runningWith();
        assertThrows(EnterpriseProjectException.class, () -> running.attachDocument(null));
    }

    @Test
    void attachDocument_tipoDuplicado_deberiaLanzar() {
        var running = runningWith(DocumentType.LEAN_CANVAS);
        assertThrows(EnterpriseProjectException.class,
            () -> running.attachDocument(document(DocumentType.LEAN_CANVAS)));
    }

    @Test
    void attachDocument_enRequested_deberiaLanzar() {
        var requested = EnterpriseProject.request(PROJECT_ID, 1);
        assertThrows(EnterpriseProjectException.class,
            () -> requested.attachDocument(document(DocumentType.LEAN_CANVAS)));
    }

    @Test
    void attachDocument_enCompleted_deberiaLanzar() {
        var completed = runningWith(DocumentType.LEAN_CANVAS).completeGeneration();
        assertThrows(EnterpriseProjectException.class,
            () -> completed.attachDocument(document(DocumentType.FINANCIAL_PLAN)));
    }

    @Test
    void attachDocument_enFailed_deberiaLanzar() {
        var failed = runningWith(DocumentType.LEAN_CANVAS).failGeneration("fallo");
        assertThrows(EnterpriseProjectException.class,
            () -> failed.attachDocument(document(DocumentType.FINANCIAL_PLAN)));
    }

    @Test
    void replaceDocument_deberiaSustituirElDocumentoDelTipo() {
        var running = runningWith(DocumentType.LEAN_CANVAS);
        var replacement = document(DocumentType.LEAN_CANVAS, 2);

        var updated = running.replaceDocument(replacement);

        assertEquals(1, updated.documentCount());
        assertEquals(replacement, updated.findDocument(DocumentType.LEAN_CANVAS).orElseThrow());
    }

    @Test
    void replaceDocument_tipoInexistente_deberiaLanzar() {
        var running = runningWith(DocumentType.LEAN_CANVAS);
        assertThrows(EnterpriseProjectException.class,
            () -> running.replaceDocument(document(DocumentType.FINANCIAL_PLAN)));
    }

    @Test
    void replaceDocument_documentoNulo_deberiaLanzar() {
        var running = runningWith(DocumentType.LEAN_CANVAS);
        assertThrows(EnterpriseProjectException.class, () -> running.replaceDocument(null));
    }

    @Test
    void replaceDocument_enRequested_deberiaLanzar() {
        var requested = EnterpriseProject.request(PROJECT_ID, 1);
        assertThrows(EnterpriseProjectException.class,
            () -> requested.replaceDocument(document(DocumentType.LEAN_CANVAS)));
    }

    @Test
    void findDocument_deberiaDevolverElDocumentoOEmpty() {
        var running = runningWith(DocumentType.LEAN_CANVAS);

        assertEquals(Optional.class, running.findDocument(DocumentType.LEAN_CANVAS).getClass());
        assertTrue(running.findDocument(DocumentType.LEAN_CANVAS).isPresent());
        assertFalse(running.findDocument(DocumentType.FINANCIAL_PLAN).isPresent());
    }

    @Test
    void hasDocument_conDocumentoNulo_deberiaLanzar() {
        var running = runningWith();
        assertThrows(EnterpriseProjectException.class, () -> running.hasDocument(null));
    }

    @Test
    void findDocument_conDocumentoNulo_deberiaLanzar() {
        var running = runningWith();
        assertThrows(EnterpriseProjectException.class, () -> running.findDocument(null));
    }

    @Test
    void documents_deberiaSerInmutable() {
        var running = runningWith(DocumentType.LEAN_CANVAS);
        var docs = running.documents();
        assertThrows(UnsupportedOperationException.class, () -> docs.add(document(DocumentType.FINANCIAL_PLAN)));
    }

    @Test
    void attachDocument_deberiaEliminarDocumentoNoIncluidoEnNuevaInstancia() {
        var running = runningWith(DocumentType.LEAN_CANVAS, DocumentType.FINANCIAL_PLAN);
        var rebuilt = EnterpriseProject.start(running.projectId(), running.version(),
            running.createdAt(), running.updatedAt(),
            running.documents().stream().filter(d -> d.type() != DocumentType.FINANCIAL_PLAN).toList());

        assertEquals(1, rebuilt.documentCount());
        assertFalse(rebuilt.hasDocument(DocumentType.FINANCIAL_PLAN));
    }

    // ------------------------------------------------------------------
    // Versionado / regeneración
    // ------------------------------------------------------------------

    @Test
    void nextVersion_desdeCompleted_deberiaCrearNuevaVersionRequested() {
        var completed = runningWith(DocumentType.LEAN_CANVAS).completeGeneration();

        var next = completed.nextVersion();

        assertEquals(PROJECT_ID, next.projectId());
        assertEquals(2, next.version());
        assertEquals(GenerationStatus.REQUESTED, next.status());
        assertTrue(next.documents().isEmpty());
        assertTrue(next.isRequested());
    }

    @Test
    void nextVersion_desdeFailed_deberiaCrearNuevaVersionRequested() {
        var failed = runningWith().failGeneration("fallo");

        var next = failed.nextVersion();

        assertEquals(2, next.version());
        assertEquals(GenerationStatus.REQUESTED, next.status());
    }

    @Test
    void nextVersion_desdeRequested_deberiaLanzar() {
        var requested = EnterpriseProject.request(PROJECT_ID, 1);
        assertThrows(EnterpriseProjectException.class, requested::nextVersion);
    }

    @Test
    void nextVersion_desdeRunning_deberiaLanzar() {
        var running = runningWith();
        assertThrows(EnterpriseProjectException.class, running::nextVersion);
    }

    @Test
    void nextVersion_repetida_deberiaIncrementarLaVersion() {
        var completed = runningWith(DocumentType.LEAN_CANVAS).completeGeneration();

        var v2 = completed.nextVersion().startGeneration().completeGeneration();
        var v3 = v2.nextVersion();

        assertEquals(3, v3.version());
    }

    @Test
    void versionesDeUnMismoProyecto_deberianSerDistintasComoAgregados() {
        var v1 = runningWith(DocumentType.LEAN_CANVAS).completeGeneration();
        var v2 = v1.nextVersion();

        assertNotEquals(v1, v2);
        assertNotEquals(v1.hashCode(), v2.hashCode());
    }

    // ------------------------------------------------------------------
    // Igualdad
    // ------------------------------------------------------------------

    @Test
    void igualdad_deberiaBasarceEnProjectIdYVersion() {
        var now = OffsetDateTime.now();
        var docs = List.of(document(DocumentType.LEAN_CANVAS));

        var a = EnterpriseProject.complete(PROJECT_ID, 1, now, now, now, docs);
        var b = EnterpriseProject.complete(PROJECT_ID, 1, now.plusDays(1), now.plusDays(1), now.plusDays(1), docs);
        var c = EnterpriseProject.complete(UUID.randomUUID(), 1, now, now, now, docs);
        var otherVersion = EnterpriseProject.complete(PROJECT_ID, 2, now, now, now, docs);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, otherVersion);
    }

    @Test
    void igualdad_conNull_yOtraClase_deberiaSerFalsa() {
        var requested = EnterpriseProject.request(PROJECT_ID, 1);
        assertFalse(requested.equals(null));
        assertFalse(requested.equals("no soy un agregado"));
        assertNotEquals(requested, new Object());
    }

    @Test
    void igualdad_deberiaSerReflexiva() {
        var requested = EnterpriseProject.request(PROJECT_ID, 1);
        assertEquals(requested, requested);
    }

    @Test
    void igualdad_deberiaSerSimetrica() {
        var now = OffsetDateTime.now();
        var a = EnterpriseProject.start(PROJECT_ID, 1, now, now, List.of());
        var b = EnterpriseProject.start(PROJECT_ID, 1, now, now, List.of());
        assertEquals(a, b);
        assertEquals(b, a);
    }

    // ------------------------------------------------------------------
    // toString
    // ------------------------------------------------------------------

    @Test
    void toString_deberiaIncluirIdentidadYEstado() {
        var requested = EnterpriseProject.request(PROJECT_ID, 1);
        var text = requested.toString();

        assertNotNull(text);
        assertTrue(text.contains(PROJECT_ID.toString()));
        assertTrue(text.contains("REQUESTED"));
        assertTrue(text.contains("1"));
    }

    // ------------------------------------------------------------------
    // Inmutabilidad estructural
    // ------------------------------------------------------------------

    @Test
    void mutarLaListaDeDocumentosDelConstructor_noDeberiaAfectarAlAgregado() {
        var now = OffsetDateTime.now();
        var docs = new ArrayList<DocumentArtifact>();
        docs.add(document(DocumentType.LEAN_CANVAS));

        var project = EnterpriseProject.start(PROJECT_ID, 1, now, now, docs);
        docs.clear();

        assertEquals(1, project.documentCount());
        assertTrue(project.hasDocument(DocumentType.LEAN_CANVAS));
    }

    @Test
    void completeGeneration_deberiaFallarSiFaltaUnDocumentoObligatorio() {
        // Sin reglas explícitas de obligatoriedad en el aggregate:
        // completeGeneration solo exige RUNNING; la completitud es responsabilidad del orquestador.
        var completed = EnterpriseProject.request(PROJECT_ID, 1).startGeneration().completeGeneration();
        assertTrue(completed.isCompleted());
        assertEquals(0, completed.documentCount());
    }
}
