package com.kinplatform.kin.pipeline;

import com.kinplatform.kin.pipeline.resilience.PipelineMetrics;
import com.kinplatform.kin.pipeline.resilience.StagePolicy;
import com.kinplatform.kin.pipeline.resilience.StageRetryPolicy;
import com.kinplatform.kin.pipeline.resilience.StageTimeoutConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineMetricsTest {

    private PipelineContext context() {
        return new PipelineContext(UUID.randomUUID(), UUID.randomUUID(), "m", List.of(), "t", "d", "c");
    }

    @Test
    void metrics_deberianCapturarDuracionYExitos() {
        var pipeline = new Pipeline(List.of(TestPipelineStages.stage("A"), TestPipelineStages.stage("B")));

        pipeline.execute(context());

        PipelineMetrics metrics = pipeline.metrics();
        assertEquals(2, metrics.totalStages());
        assertEquals(2, metrics.successfulStages());
        assertEquals(0, metrics.failedStages());
        assertEquals(1.0, metrics.successRate(), 1e-9);
        assertTrue(metrics.totalDurationMillis() >= 0);
        assertEquals("Pipeline", metrics.generatedBy());
        assertTrue(metrics.statsFor("A").orElseThrow().success());
    }

    @Test
    void metrics_deberianCapturarReintentos() {
        AtomicInteger executions = new AtomicInteger();
        var pipeline = new Pipeline(
            List.of(TestPipelineStages.stage("S", () -> {
                if (executions.incrementAndGet() <= 1) {
                    throw new IllegalStateException("transitorio");
                }
            })),
            List.of(StagePolicy.retry("S", 3, 1_000L)),
            StageRetryPolicy.fixed(3, 0, Set.of("S")),
            StageTimeoutConfig.defaultTimeout(StagePolicy.DEFAULT_TIMEOUT_MILLIS));

        pipeline.execute(context());

        PipelineMetrics metrics = pipeline.metrics();
        assertEquals(1, metrics.totalRetries());
        assertEquals(2, metrics.statsFor("S").orElseThrow().attempts());
        assertEquals(1, metrics.successfulStages());
    }

    @Test
    void metrics_deberianCapturarFalloConPoliticaSkip() {
        var pipeline = new Pipeline(
            List.of(TestPipelineStages.failing("F", new IllegalStateException("boom")), TestPipelineStages.stage("B")),
            List.of(StagePolicy.skipOnFailure("F")),
            StageRetryPolicy.none(),
            StageTimeoutConfig.defaultTimeout(StagePolicy.DEFAULT_TIMEOUT_MILLIS));

        pipeline.execute(context());

        PipelineMetrics metrics = pipeline.metrics();
        assertEquals(1, metrics.failedStages());
        assertEquals(1, metrics.successfulStages());
        assertEquals(0.5, metrics.successRate(), 1e-9);
        assertTrue(metrics.explanation().contains("2 stage(s)"));
    }

    @Test
    void metrics_antesDeEjecutar_deberianSerVacias() {
        var pipeline = new Pipeline(List.of(TestPipelineStages.stage("A")));

        assertTrue(pipeline.metrics().isEmpty());
        assertEquals(0.0, pipeline.metrics().successRate(), 1e-9);
        assertEquals(0, pipeline.metrics().totalStages());
    }

    @Test
    void metrics_deberianRegistrarElFalloCuandoElPipelineLanza() {
        var pipeline = new Pipeline(List.of(TestPipelineStages.failing("F", new IllegalStateException("boom"))));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> pipeline.execute(context()));

        PipelineMetrics metrics = pipeline.metrics();
        assertEquals(1, metrics.totalStages());
        assertEquals(1, metrics.failedStages());
        assertTrue(metrics.statsFor("F").orElseThrow().errorMessage().contains("boom"));
    }
}
