package com.kinplatform.kin.pipeline.resilience;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineExecutionExceptionTest {

    @Test
    void conCausa_deberiaTransportarStageYClase() {
        var cause = new IllegalStateException("boom");
        var ex = new PipelineExecutionException("Scoring", PipelineExecutionException.FailureKind.UNEXPECTED, cause);

        assertEquals("Scoring", ex.stageName());
        assertEquals(PipelineExecutionException.FailureKind.UNEXPECTED, ex.kind());
        assertSame(cause, ex.getCause());
        assertFalse(ex.isTimeout());
        assertFalse(ex.isRetryExhausted());
    }

    @Test
    void timeout_deberiaClasificarseComoTimeout() {
        var ex = new PipelineExecutionException("Consultor", PipelineExecutionException.FailureKind.TIMEOUT, new RuntimeException());

        assertTrue(ex.isTimeout());
        assertFalse(ex.isRetryExhausted());
    }

    @Test
    void retryExhausted_deberiaClasificarseComoRetryAgotado() {
        var ex = new PipelineExecutionException("Conocimiento", PipelineExecutionException.FailureKind.RETRY_EXHAUSTED, new RuntimeException());

        assertTrue(ex.isRetryExhausted());
        assertFalse(ex.isTimeout());
    }

    @Test
    void kindNulo_deberiaNormalizarAUNEXPECTED() {
        var ex = new PipelineExecutionException("S", null, new RuntimeException());

        assertEquals(PipelineExecutionException.FailureKind.UNEXPECTED, ex.kind());
    }

    @Test
    void soloMensaje_deberiaTransportarMensaje() {
        var ex = new PipelineExecutionException("S", PipelineExecutionException.FailureKind.TIMEOUT, "mensaje directo");

        assertEquals("mensaje directo", ex.getMessage());
        assertNull(ex.getCause());
        assertEquals("S", ex.stageName());
    }

    @Test
    void mensajePorDefecto_deberiaIncluirStageYClase() {
        var ex = new PipelineExecutionException("Riesgos", PipelineExecutionException.FailureKind.TIMEOUT, new RuntimeException());

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("Riesgos"));
        assertTrue(ex.getMessage().contains("timeout"));
    }

    @Test
    void toString_deberiaIncluirElNombreDeLaExcepcionYElMensaje() {
        var ex = new PipelineExecutionException("S", PipelineExecutionException.FailureKind.TIMEOUT, new RuntimeException());

        assertTrue(ex.toString().contains("PipelineExecutionException"));
        assertTrue(ex.toString().contains("timeout"));
    }
}
