package com.kinplatform.kin.pipeline;

import com.kinplatform.kin.pipeline.resilience.PipelineExecutionException;
import com.kinplatform.kin.pipeline.resilience.StagePolicy;
import com.kinplatform.kin.pipeline.resilience.StageRetryPolicy;
import com.kinplatform.kin.pipeline.resilience.StageTimeoutConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineRetryTest {

    private PipelineContext context() {
        return new PipelineContext(UUID.randomUUID(), UUID.randomUUID(), "m", List.of(), "t", "d", "c");
    }

    private Pipeline resilient(List<com.kinplatform.kin.pipeline.PipelineStage> stages,
                               List<StagePolicy> policies, StageRetryPolicy retry) {
        return new Pipeline(stages, policies, retry,
            StageTimeoutConfig.defaultTimeout(StagePolicy.DEFAULT_TIMEOUT_MILLIS));
    }

    @Test
    void retry_deberiaReintentarYTenerExito() {
        AtomicInteger executions = new AtomicInteger();
        var pipeline = resilient(
            List.of(TestPipelineStages.stage("S", () -> {
                if (executions.incrementAndGet() <= 2) {
                    throw new IllegalStateException("transitorio");
                }
            })),
            List.of(StagePolicy.retry("S", 3, 1_000L)),
            StageRetryPolicy.fixed(3, 0, Set.of("S")));

        var ctx = pipeline.execute(context());

        assertTrue(ctx.completed());
        assertEquals(3, executions.get());
        var stat = pipeline.metrics().statsFor("S").orElseThrow();
        assertTrue(stat.success());
        assertEquals(3, stat.attempts());
        assertEquals(2, stat.retries());
    }

    @Test
    void retry_deberiaAgotarseYLanzarRetryExhausted() {
        AtomicInteger executions = new AtomicInteger();
        var pipeline = resilient(
            List.of(TestPipelineStages.stage("S", () -> {
                executions.incrementAndGet();
                throw new IllegalStateException("siempre falla");
            })),
            List.of(StagePolicy.retry("S", 2, 1_000L)),
            StageRetryPolicy.fixed(5, 0, Set.of("S")));

        PipelineExecutionException ex = assertThrows(PipelineExecutionException.class,
            () -> pipeline.execute(context()));

        assertEquals(PipelineExecutionException.FailureKind.RETRY_EXHAUSTED, ex.kind());
        assertEquals("S", ex.stageName());
        assertEquals(4, executions.get());
        assertFalse(pipeline.metrics().statsFor("S").orElseThrow().success());
    }

    @Test
    void retry_deberiaNoReintentar_cuandoElStageNoEsElegible() {
        AtomicInteger executions = new AtomicInteger();
        var pipeline = resilient(
            List.of(TestPipelineStages.stage("S", () -> {
                executions.incrementAndGet();
                throw new IllegalStateException("boom");
            })),
            List.of(StagePolicy.retry("S", 3, 1_000L)),
            StageRetryPolicy.fixed(3, 0, Set.of("Otro")));

        assertThrows(PipelineExecutionException.class, () -> pipeline.execute(context()));

        assertEquals(1, executions.get());
    }

    @Test
    void retry_deberiaNoReintentar_cuandoLaPoliticaEsFail() {
        AtomicInteger executions = new AtomicInteger();
        var pipeline = resilient(
            List.of(TestPipelineStages.stage("S", () -> {
                executions.incrementAndGet();
                throw new IllegalStateException("boom");
            })),
            List.of(StagePolicy.failFast("S")),
            StageRetryPolicy.fixed(3, 0, Set.of("S")));

        PipelineExecutionException ex = assertThrows(PipelineExecutionException.class,
            () -> pipeline.execute(context()));

        assertEquals(1, executions.get());
        assertEquals(PipelineExecutionException.FailureKind.UNEXPECTED, ex.kind());
    }

    @Test
    void retry_deberiaRegistrarReintentosEnMetricas() {
        AtomicInteger executions = new AtomicInteger();
        var pipeline = resilient(
            List.of(TestPipelineStages.stage("S", () -> {
                if (executions.incrementAndGet() <= 1) {
                    throw new IllegalStateException("transitorio");
                }
            })),
            List.of(StagePolicy.retry("S", 3, 1_000L)),
            StageRetryPolicy.exponential(3, 0, Set.of("S")));

        pipeline.execute(context());

        assertEquals(1, pipeline.metrics().totalRetries());
        assertEquals(2, pipeline.metrics().statsFor("S").orElseThrow().attempts());
    }
}
