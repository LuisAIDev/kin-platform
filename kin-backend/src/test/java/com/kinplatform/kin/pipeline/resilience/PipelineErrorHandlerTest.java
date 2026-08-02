package com.kinplatform.kin.pipeline.resilience;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineErrorHandlerTest {

    @Test
    void classify_conTimeout_deberiaConstruirExcepcionTIMEOUT() {
        var cause = new TimeoutException("timeout");
        var ex = PipelineErrorHandler.classify("Consultor", cause, true, 1);

        assertEquals("Consultor", ex.stageName());
        assertEquals(PipelineExecutionException.FailureKind.TIMEOUT, ex.kind());
        assertSame(cause, ex.getCause());
    }

    @Test
    void classify_sinTimeout_deberiaConstruirExcepcionUNEXPECTED() {
        var cause = new IllegalStateException("boom");
        var ex = PipelineErrorHandler.classify("Evaluador", cause, false, 1);

        assertEquals(PipelineExecutionException.FailureKind.UNEXPECTED, ex.kind());
        assertSame(cause, ex.getCause());
    }

    @Test
    void classify_conCausaTimeoutAnidada_deberiaDetectarTimeout() {
        var cause = new RuntimeException(new TimeoutException("timeout"));
        var ex = PipelineErrorHandler.classify("S", cause, false, 1);

        assertEquals(PipelineExecutionException.FailureKind.TIMEOUT, ex.kind());
    }

    @Test
    void isTimeout_directo_deberiaSerCierto() {
        assertTrue(PipelineErrorHandler.isTimeout(new TimeoutException("t")));
    }

    @Test
    void isTimeout_anidado_deberiaSerCierto() {
        var error = new RuntimeException(new RuntimeException(new TimeoutException("t")));
        assertTrue(PipelineErrorHandler.isTimeout(error));
    }

    @Test
    void isTimeout_errorComun_deberiaSerFalso() {
        assertFalse(PipelineErrorHandler.isTimeout(new IllegalStateException("boom")));
    }

    @Test
    void isTimeout_nulo_deberiaSerFalso() {
        assertFalse(PipelineErrorHandler.isTimeout(null));
    }

    @Test
    void isTimeout_cadenaCiclica_deberiaTerminarSinDesbordar() {
        var a = new RuntimeException("a");
        var b = new RuntimeException("b");
        a.initCause(b);
        b.initCause(a);

        assertFalse(PipelineErrorHandler.isTimeout(a));
    }

    @Test
    void kindOf_deberiaClasificarTimeoutPorBanderaOCausa() {
        assertEquals(PipelineExecutionException.FailureKind.TIMEOUT,
            PipelineErrorHandler.kindOf(new RuntimeException(), true));
        assertEquals(PipelineExecutionException.FailureKind.TIMEOUT,
            PipelineErrorHandler.kindOf(new TimeoutException(), false));
        assertEquals(PipelineExecutionException.FailureKind.UNEXPECTED,
            PipelineErrorHandler.kindOf(new RuntimeException(), false));
    }
}
