package com.kinplatform.kin.pipeline;

import com.kinplatform.kin.pipeline.resilience.PipelineExecutionException;
import com.kinplatform.kin.pipeline.resilience.StagePolicy;
import com.kinplatform.kin.pipeline.resilience.StageRetryPolicy;
import com.kinplatform.kin.pipeline.resilience.StageTimeoutConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineTimeoutTest {

    private PipelineContext context() {
        return new PipelineContext(UUID.randomUUID(), UUID.randomUUID(), "m", List.of(), "t", "d", "c");
    }

    @Test
    void timeout_conOverrunYAccionSkip_deberiaMarcarTimedOutYContinuar() {
        var pipeline = new Pipeline(
            List.of(TestPipelineStages.slow("Lento", 150L), TestPipelineStages.stage("B")),
            List.of(StagePolicy.failFast("Lento")),
            StageRetryPolicy.none(),
            new StageTimeoutConfig(Map.of(), 50L, StageTimeoutConfig.TimeoutAction.SKIP));

        var ctx = pipeline.execute(context());

        assertTrue(ctx.completed());
        var stat = pipeline.metrics().statsFor("Lento").orElseThrow();
        assertFalse(stat.success());
        assertTrue(stat.timedOut());
        assertEquals(1, pipeline.metrics().timedOutStages());
        assertTrue(pipeline.metrics().statsFor("B").isPresent());
    }

    @Test
    void timeout_conCausaTimeoutYAccionFail_deberiaLanzarExcepcionTIMEOUT() {
        var pipeline = new Pipeline(
            List.of(TestPipelineStages.failing("S", new RuntimeException(new TimeoutException("timeout")))),
            List.of(StagePolicy.failFast("S")),
            StageRetryPolicy.none(),
            new StageTimeoutConfig(Map.of(), 5_000L, StageTimeoutConfig.TimeoutAction.FAIL));

        PipelineExecutionException ex = assertThrows(PipelineExecutionException.class,
            () -> pipeline.execute(context()));

        assertEquals(PipelineExecutionException.FailureKind.TIMEOUT, ex.kind());
        assertEquals("S", ex.stageName());
        assertTrue(ex.isTimeout());
    }

    @Test
    void timeout_conOverrunYAccionFail_deberiaLanzarExcepcionTIMEOUT() {
        var pipeline = new Pipeline(
            List.of(TestPipelineStages.slow("Lento", 120L)),
            List.of(StagePolicy.failFast("Lento")),
            StageRetryPolicy.none(),
            new StageTimeoutConfig(Map.of(), 40L, StageTimeoutConfig.TimeoutAction.FAIL));

        PipelineExecutionException ex = assertThrows(PipelineExecutionException.class,
            () -> pipeline.execute(context()));

        assertEquals(PipelineExecutionException.FailureKind.TIMEOUT, ex.kind());
        assertTrue(pipeline.metrics().statsFor("Lento").orElseThrow().timedOut());
    }

    @Test
    void timeout_conRetry_deberiaReintentarAntesDeTerminar() {
        AtomicInteger executions = new AtomicInteger();
        var pipeline = new Pipeline(
            List.of(TestPipelineStages.stage("S", () -> {
                executions.incrementAndGet();
                if (executions.get() <= 2) {
                    throw new RuntimeException(new TimeoutException("timeout"));
                }
            })),
            List.of(StagePolicy.retry("S", 3, 5_000L)),
            StageRetryPolicy.fixed(3, 0, Set.of("S")),
            new StageTimeoutConfig(Map.of(), 5_000L, StageTimeoutConfig.TimeoutAction.RETRY));

        var ctx = pipeline.execute(context());

        assertTrue(ctx.completed());
        assertEquals(3, executions.get());
        var stat = pipeline.metrics().statsFor("S").orElseThrow();
        assertTrue(stat.success());
        assertEquals(3, stat.attempts());
    }

    @Test
    void timeout_deberiaUsarTimeoutEspecificoPorStage() {
        var config = new StageTimeoutConfig(
            java.util.Map.of("Rapido", 10_000L), 40L, StageTimeoutConfig.TimeoutAction.SKIP);

        assertEquals(10_000L, config.timeoutMillisFor("Rapido"));
        assertEquals(40L, config.timeoutMillisFor("Otro"));
    }

    @Test
    void timeout_conRetryAgotado_deberiaLanzarExcepcionTimeout() {
        AtomicInteger executions = new AtomicInteger();
        var pipeline = new Pipeline(
            List.of(TestPipelineStages.stage("S", () -> {
                executions.incrementAndGet();
                throw new RuntimeException(new TimeoutException("t"));
            })),
            List.of(StagePolicy.retry("S", 0, 5_000L)),
            StageRetryPolicy.fixed(5, 0, Set.of("S")),
            new StageTimeoutConfig(Map.of(), 5_000L, StageTimeoutConfig.TimeoutAction.RETRY));

        PipelineExecutionException ex = assertThrows(PipelineExecutionException.class,
            () -> pipeline.execute(context()));

        assertEquals(PipelineExecutionException.FailureKind.TIMEOUT, ex.kind());
        assertEquals(2, executions.get());
        assertTrue(pipeline.metrics().statsFor("S").orElseThrow().timedOut());
    }
}
