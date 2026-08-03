package com.kinplatform.kin.enterprise.progress;

import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseProgressPublisherTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    private final List<EnterpriseProgressEvent> published = new ArrayList<>();
    private final EnterpriseProgressPublisher publisher =
        new EnterpriseProgressPublisher(published::add);

    @Test
    void publishFor_conProyectoCompletado_deberiaEmitirDocumentosYCompletado() {
        var project = com.kinplatform.kin.enterprise.aggregate.EnterpriseProject.complete(
            PROJECT_ID, 1, java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now(),
            java.time.OffsetDateTime.now(),
            java.util.List.of(
                WebProgressFixtures.document(DocumentType.LEAN_CANVAS, 1),
                WebProgressFixtures.document(DocumentType.KPI, 1)));

        publisher.publishFor(project);

        assertEquals(3, published.size());
        assertEquals(EnterpriseProgressState.DOCUMENT_GENERATED, published.get(0).state());
        assertEquals(DocumentType.LEAN_CANVAS, published.get(0).documentType());
        assertEquals(EnterpriseProgressState.DOCUMENT_GENERATED, published.get(1).state());
        assertEquals(DocumentType.KPI, published.get(1).documentType());
        assertEquals(EnterpriseProgressState.COMPLETED, published.get(2).state());
        assertTrue(published.get(2).state().isTerminal());
    }

    @Test
    void publishFor_conProyectoRequested_deberiaEmitirSolicitado() {
        var project = com.kinplatform.kin.enterprise.aggregate.EnterpriseProject.request(PROJECT_ID, 1);

        publisher.publishFor(project);

        assertEquals(1, published.size());
        assertEquals(EnterpriseProgressState.REQUESTED, published.get(0).state());
    }

    @Test
    void publishFor_conProyectoRunning_deberiaEmitirEnCurso() {
        var now = java.time.OffsetDateTime.now();
        var project = com.kinplatform.kin.enterprise.aggregate.EnterpriseProject.start(
            PROJECT_ID, 1, now, now, java.util.List.of());

        publisher.publishFor(project);

        assertEquals(1, published.size());
        assertEquals(EnterpriseProgressState.RUNNING, published.get(0).state());
    }

    @Test
    void publishFor_conProyectoFallido_deberiaEmitirDocumentosYFallo() {
        var now = java.time.OffsetDateTime.now();
        var project = com.kinplatform.kin.enterprise.aggregate.EnterpriseProject.fail(
            PROJECT_ID, 1, now, now, "motor falló",
            java.util.List.of(WebProgressFixtures.document(DocumentType.ROADMAP, 1)));

        publisher.publishFor(project);

        assertEquals(2, published.size());
        assertEquals(EnterpriseProgressState.DOCUMENT_GENERATED, published.get(0).state());
        assertEquals(EnterpriseProgressState.FAILED, published.get(1).state());
        assertTrue(published.get(1).message().contains("motor falló"));
        assertTrue(published.get(1).state().isTerminal());
    }

    @Test
    void publishFor_conProyectoNulo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> publisher.publishFor(null));
    }

    @Test
    void constructor_conSinkNulo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> new EnterpriseProgressPublisher(null));
    }
}
