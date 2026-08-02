package com.kinplatform.kin.pipeline;

import com.kinplatform.kin.pipeline.resilience.PipelineExecutionException;
import com.kinplatform.kin.pipeline.resilience.StagePolicy;
import com.kinplatform.kin.pipeline.resilience.StageRetryPolicy;
import com.kinplatform.kin.pipeline.resilience.StageTimeoutConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineFailureTest {

    private PipelineContext context() {
        return new PipelineContext(UUID.randomUUID(), UUID.randomUUID(), "m", List.of(), "t", "d", "c");
    }

    @Test
    void failFast_deberiaLanzarPipelineExecutionException() {
        var pipeline = new Pipeline(List.of(TestPipelineStages.failing("F", new IllegalStateException("boom"))));

        PipelineExecutionException ex = assertThrows(PipelineExecutionException.class,
            () -> pipeline.execute(context()));

        assertEquals("F", ex.stageName());
        assertEquals(PipelineExecutionException.FailureKind.UNEXPECTED, ex.kind());
        assertEquals("boom", ex.getCause().getMessage());
    }

    @Test
    void failFast_deberiaDetenerLaEjecucionEnElPrimerFallo() {
        AtomicInteger ejecutados = new AtomicInteger();
        var pipeline = new Pipeline(List.of(
            TestPipelineStages.failing("F", new IllegalStateException("boom")),
            TestPipelineStages.stage("B", ejecutados::incrementAndGet)));

        assertThrows(PipelineExecutionException.class, () -> pipeline.execute(context()));

        assertEquals(0, ejecutados.get());
        assertEquals(1, pipeline.metrics().totalStages());
        assertTrue(pipeline.metrics().statsFor("F").isPresent());
    }

    @Test
    void failFast_deberiaClasificarComoTimeout_porCausa() {
        var pipeline = new Pipeline(List.of(
            TestPipelineStages.failing("F", new RuntimeException(new TimeoutException("t")))));

        PipelineExecutionException ex = assertThrows(PipelineExecutionException.class,
            () -> pipeline.execute(context()));

        assertEquals(PipelineExecutionException.FailureKind.TIMEOUT, ex.kind());
        assertTrue(ex.isTimeout());
    }

    @Test
    void failFast_conFalloAnidado_deberiaConservarLaCausa() {
        var cause = new IllegalStateException("causa raíz");
        var pipeline = new Pipeline(List.of(TestPipelineStages.failing("F", cause)));

        PipelineExecutionException ex = assertThrows(PipelineExecutionException.class,
            () -> pipeline.execute(context()));

        assertTrue(ex.getCause() == cause);
        assertTrue(pipeline.metrics().statsFor("F").orElseThrow().errorMessage().contains("causa raíz"));
    }

    @Test
    void skip_deberiaPermitirContinuarYNoLanzar() {
        var pipeline = new Pipeline(
            List.of(TestPipelineStages.failing("F", new IllegalStateException("boom")), TestPipelineStages.stage("B")),
            List.of(StagePolicy.skipOnFailure("F")),
            StageRetryPolicy.none(),
            StageTimeoutConfig.defaultTimeout(StagePolicy.DEFAULT_TIMEOUT_MILLIS));

        var ctx = pipeline.execute(context());

        assertTrue(ctx.completed());
        assertTrue(pipeline.metrics().statsFor("B").isPresent());
        assertEquals(1, pipeline.metrics().failedStages());
    }

    @Test
    void retryAgotado_deberiaLanzarRetryExhausted() {
        var pipeline = new Pipeline(
            List.of(TestPipelineStages.failing("S", new IllegalStateException("siempre"))),
            List.of(StagePolicy.retry("S", 2, 1_000L)),
            StageRetryPolicy.fixed(5, 0, Set.of("S")),
            StageTimeoutConfig.defaultTimeout(StagePolicy.DEFAULT_TIMEOUT_MILLIS));

        PipelineExecutionException ex = assertThrows(PipelineExecutionException.class,
            () -> pipeline.execute(context()));

        assertEquals(PipelineExecutionException.FailureKind.RETRY_EXHAUSTED, ex.kind());
        assertEquals("S", ex.stageName());
    }

    @Test
    void retryConRetryGlobalDeshabilitado_deberiaFallarRapido() {
        java.util.concurrent.atomic.AtomicInteger executions = new java.util.concurrent.atomic.AtomicInteger();
        var pipeline = new Pipeline(
            List.of(TestPipelineStages.stage("S", () -> {
                executions.incrementAndGet();
                throw new IllegalStateException("boom");
            })),
            List.of(StagePolicy.retry("S", 3, 1_000L)),
            StageRetryPolicy.none(),
            StageTimeoutConfig.defaultTimeout(StagePolicy.DEFAULT_TIMEOUT_MILLIS));

        PipelineExecutionException ex = assertThrows(PipelineExecutionException.class,
            () -> pipeline.execute(context()));

        assertEquals(PipelineExecutionException.FailureKind.UNEXPECTED, ex.kind());
        assertEquals(1, executions.get());
    }
}
