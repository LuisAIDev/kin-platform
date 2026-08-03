package com.kinplatform.kin.enterprise.progress;

import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseProgressEventTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    @Test
    void of_deberiaCrearUnEventoConInstanteActual() {
        var event = EnterpriseProgressEvent.of(PROJECT_ID, 1,
            EnterpriseProgressState.RUNNING, null, "en curso");

        assertTrue(event.timestamp() != null);
        assertTrue(event.timestamp().isAfter(Instant.now().minusSeconds(1)));
        assertTrue(event.timestamp().isBefore(Instant.now().plusSeconds(1)));
        assertFalse(event.state().isTerminal());
    }

    @Test
    void estadoDocumentoGenerado_requiereTipoDeDocumento() {
        assertThrows(IllegalArgumentException.class, () -> EnterpriseProgressEvent.of(
            PROJECT_ID, 1, EnterpriseProgressState.DOCUMENT_GENERATED, null, "documento"));
    }

    @Test
    void proyectoNulo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> EnterpriseProgressEvent.of(
            null, 1, EnterpriseProgressState.RUNNING, null, null));
    }

    @Test
    void versionInvalida_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> EnterpriseProgressEvent.of(
            PROJECT_ID, 0, EnterpriseProgressState.RUNNING, null, null));
    }

    @Test
    void estadoNulo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> new EnterpriseProgressEvent(
            PROJECT_ID, 1, null, Instant.now(), null, null));
    }

    @Test
    void estadosTerminales_deberianSerCompletadoYFallido() {
        assertTrue(EnterpriseProgressState.COMPLETED.isTerminal());
        assertTrue(EnterpriseProgressState.FAILED.isTerminal());
        assertFalse(EnterpriseProgressState.REQUESTED.isTerminal());
        assertFalse(EnterpriseProgressState.RUNNING.isTerminal());
        assertFalse(EnterpriseProgressState.DOCUMENT_GENERATED.isTerminal());
    }

    @Test
    void of_conDocumentoGenerado_deberiaConservarElTipo() {
        var event = EnterpriseProgressEvent.of(PROJECT_ID, 2,
            EnterpriseProgressState.DOCUMENT_GENERATED, DocumentType.LEAN_CANVAS, "canvas");

        assertTrue(event.documentType() == DocumentType.LEAN_CANVAS);
        assertTrue(event.message().equals("canvas"));
    }
}
