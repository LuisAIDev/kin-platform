package com.kinplatform.kin.pipeline;

import com.kinplatform.kin.pipeline.resilience.PipelineMetrics;
import com.kinplatform.kin.pipeline.resilience.PipelineExecutionException;
import com.kinplatform.kin.pipeline.resilience.PipelineErrorHandler;
import com.kinplatform.kin.pipeline.resilience.StagePolicy;
import com.kinplatform.kin.pipeline.resilience.StageRetryPolicy;
import com.kinplatform.kin.pipeline.resilience.StageTimeoutConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineResilienceTest {

    private PipelineContext context() {
        return new PipelineContext(UUID.randomUUID(), UUID.randomUUID(), "m", List.of(), "t", "d", "c");
    }

    @Test
    void pipeline_conConstructorDeCompatibilidad_deberiaEjecutarYCompletar() {
        var pipeline = new Pipeline(List.of(TestPipelineStages.stage("A"), TestPipelineStages.stage("B")));

        var ctx = pipeline.execute(context());

        assertTrue(ctx.completed());
        assertEquals(2, pipeline.metrics().totalStages());
        assertEquals(2, pipeline.metrics().successfulStages());
        assertEquals(0, pipeline.metrics().failedStages());
    }

    @Test
    void pipeline_deberiaRegistrarMetricasPorStage() {
        var pipeline = new Pipeline(List.of(TestPipelineStages.stage("A"), TestPipelineStages.stage("B")));

        pipeline.execute(context());

        PipelineMetrics metrics = pipeline.metrics();
        assertTrue(metrics.statsFor("A").isPresent());
        assertTrue(metrics.statsFor("B").isPresent());
        assertTrue(metrics.statsFor("A").orElseThrow().success());
        assertEquals(0, metrics.totalRetries());
    }

    @Test
    void pipeline_deberiaOmitirStagesNoSoportados() {
        var pipeline = new Pipeline(List.of(
            TestPipelineStages.stage("A"), TestPipelineStages.unsupported("X"), TestPipelineStages.stage("B")));

        pipeline.execute(context());

        assertEquals(2, pipeline.metrics().totalStages());
        assertTrue(pipeline.metrics().statsFor("X").isEmpty());
    }

    @Test
    void pipeline_conStagesVacios_deberiaMarcarCompletadoYMetricasVacias() {
        var pipeline = new Pipeline(List.of());

        var ctx = pipeline.execute(context());

        assertTrue(ctx.completed());
        assertTrue(pipeline.metrics().isEmpty());
    }

    @Test
    void pipeline_conPoliticaSkip_deberiaContinuarYRegistrarElFallo() {
        var pipeline = new Pipeline(
            List.of(TestPipelineStages.failing("F", new IllegalStateException("boom")), TestPipelineStages.stage("B")),
            List.of(StagePolicy.skipOnFailure("F")),
            StageRetryPolicy.none(),
            StageTimeoutConfig.defaultTimeout(StagePolicy.DEFAULT_TIMEOUT_MILLIS));

        var ctx = pipeline.execute(context());

        assertTrue(ctx.completed());
        assertEquals(1, pipeline.metrics().failedStages());
        assertEquals(1, pipeline.metrics().successfulStages());
        assertTrue(pipeline.metrics().statsFor("F").orElseThrow().errorMessage().contains("boom"));
    }

    @Test
    void pipeline_deberiaDetenerseCuandoUnStageMarcaCompletado() {
        var pipeline = new Pipeline(List.of(
            TestPipelineStages.stage("A"), TestPipelineStages.completingStage("C"), TestPipelineStages.stage("Z")));

        pipeline.execute(context());

        assertEquals(2, pipeline.metrics().totalStages());
        assertTrue(pipeline.metrics().statsFor("Z").isEmpty());
    }

    @Test
    void pipeline_deberiaMantenerLaFirmaYElContextoDeEntrada() {
        var pipeline = new Pipeline(List.of(TestPipelineStages.stage("A")));
        var input = context();

        PipelineContext output = pipeline.execute(input);

        assertSame(input, output);
    }

    @Test
    void pipeline_metricasAntesDeEjecutar_deberianSerVacias() {
        var pipeline = new Pipeline(List.of(TestPipelineStages.stage("A")));

        assertTrue(pipeline.metrics().isEmpty());
    }

    @Test
    void pipeline_conFalloPoliticaFail_deberiaLanzarExcepcionClasificada() {
        var pipeline = new Pipeline(List.of(TestPipelineStages.failing("F", new IllegalStateException("boom"))));

        PipelineExecutionException ex = org.junit.jupiter.api.Assertions.assertThrows(
            PipelineExecutionException.class, () -> pipeline.execute(context()));

        assertEquals("F", ex.stageName());
        assertEquals(PipelineExecutionException.FailureKind.UNEXPECTED, ex.kind());
    }

    @Test
    void pipeline_deberiaRegistrarMetricasDelStageQueFallo() {
        var pipeline = new Pipeline(List.of(TestPipelineStages.failing("F", new IllegalStateException("boom"))));

        org.junit.jupiter.api.Assertions.assertThrows(
            PipelineExecutionException.class, () -> pipeline.execute(context()));

        assertEquals(1, pipeline.metrics().totalStages());
        assertEquals(1, pipeline.metrics().failedStages());
        assertTrue(pipeline.metrics().statsFor("F").isPresent());
    }

    @Test
    void pipeline_clasificador_deberiaDetectarCausaTimeout() {
        java.util.concurrent.TimeoutException cause = new java.util.concurrent.TimeoutException("t");

        assertTrue(PipelineErrorHandler.isTimeout(cause));
        assertEquals(PipelineExecutionException.FailureKind.TIMEOUT,
            PipelineErrorHandler.kindOf(cause, false));
    }
}
