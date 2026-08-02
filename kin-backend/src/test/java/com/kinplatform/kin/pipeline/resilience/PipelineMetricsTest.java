package com.kinplatform.kin.pipeline.resilience;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineMetricsTest {

    private PipelineMetrics metrics() {
        return new PipelineMetrics(List.of(
            StageExecutionStats.success("Analizador", 10L, 1),
            StageExecutionStats.failure("Evaluador", 5L, 2, "boom"),
            StageExecutionStats.timedOut("Estratega", 3_000L, 1),
            StageExecutionStats.success("Consultor", 20L, 3)
        ), 3_035L);
    }

    @Test
    void empty_deberiaSerVacia() {
        var metrics = PipelineMetrics.empty();

        assertTrue(metrics.isEmpty());
        assertEquals(0, metrics.totalStages());
        assertEquals(0.0, metrics.successRate(), 1e-9);
        assertEquals(0L, metrics.totalDurationMillis());
        assertEquals("Pipeline", metrics.generatedBy());
        assertEquals("v1", metrics.engineVersion());
        assertTrue(metrics.stageStats().isEmpty());
    }

    @Test
    void totales_deberianAgregarCorrectamente() {
        var metrics = metrics();

        assertEquals(4, metrics.totalStages());
        assertEquals(2, metrics.successfulStages());
        assertEquals(2, metrics.failedStages());
        assertEquals(1, metrics.timedOutStages());
        assertEquals(3, metrics.totalRetries());
    }

    @Test
    void successRate_deberiaCalcularTasaDeExito() {
        assertEquals(0.5, metrics().successRate(), 1e-9);
        assertEquals(1.0, new PipelineMetrics(List.of(
            StageExecutionStats.success("S", 1L, 1)), 1L).successRate(), 1e-9);
    }

    @Test
    void statsFor_deberiaBuscarPorNombre() {
        var metrics = metrics();

        assertTrue(metrics.statsFor("Analizador").isPresent());
        assertEquals("Analizador", metrics.statsFor("Analizador").orElseThrow().stageName());
        assertTrue(metrics.statsFor("Inexistente").isEmpty());
    }

    @Test
    void confidence_explanation_deberianDelegarEnLaTasa() {
        var metrics = metrics();

        assertEquals(0.5, metrics.confidence(), 1e-9);
        assertTrue(metrics.explanation().contains("4 stage(s)"));
        assertTrue(metrics.explanation().contains("2 exitosos"));
        assertEquals("Sin métricas de pipeline.", PipelineMetrics.empty().explanation());
    }

    @Test
    void constructor_deberiaNormalizarStageStatsNuloAVacio() {
        var metrics = new PipelineMetrics(null, 1L);

        assertTrue(metrics.isEmpty());
        assertEquals(0, metrics.totalStages());
    }

    @Test
    void constructor_deberiaNormalizarDuracionNegativaACero() {
        var metrics = new PipelineMetrics(List.of(), -5L);

        assertEquals(0L, metrics.totalDurationMillis());
    }

    @Test
    void equals_hashCode_toString_deberianComportarseComoRecord() {
        var a = metrics();
        var b = metrics();
        var c = PipelineMetrics.empty();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertTrue(a.toString().contains("Analizador"));
        assertFalse(a.isEmpty());
    }
}
