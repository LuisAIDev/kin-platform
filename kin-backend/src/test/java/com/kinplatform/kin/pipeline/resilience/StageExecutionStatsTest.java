package com.kinplatform.kin.pipeline.resilience;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StageExecutionStatsTest {

    @Test
    void success_deberiaCrearEstadisticaExitosa() {
        var stats = StageExecutionStats.success("Analizador", 120L, 1);

        assertEquals("Analizador", stats.stageName());
        assertTrue(stats.success());
        assertEquals(120L, stats.durationMillis());
        assertEquals(1, stats.attempts());
        assertFalse(stats.timedOut());
        assertEquals("", stats.errorMessage());
    }

    @Test
    void failure_deberiaCrearEstadisticaFallida() {
        var stats = StageExecutionStats.failure("Riesgos", 50L, 3, "error de análisis");

        assertFalse(stats.success());
        assertEquals("error de análisis", stats.errorMessage());
        assertFalse(stats.timedOut());
    }

    @Test
    void timedOut_deberiaCrearEstadisticaConTimeout() {
        var stats = StageExecutionStats.timedOut("Consultor", 5_000L, 1);

        assertFalse(stats.success());
        assertTrue(stats.timedOut());
        assertEquals("timeout", stats.errorMessage());
    }

    @Test
    void constructor_deberiaRechazarStageNameNuloOVacio() {
        assertThrows(IllegalArgumentException.class,
            () -> new StageExecutionStats(null, true, 1L, 1, false, ""));
        assertThrows(IllegalArgumentException.class,
            () -> new StageExecutionStats(" ", true, 1L, 1, false, ""));
    }

    @Test
    void constructor_deberiaRechazarDuracionNegativa() {
        assertThrows(IllegalArgumentException.class,
            () -> new StageExecutionStats("S", true, -1L, 1, false, ""));
    }

    @Test
    void constructor_deberiaNormalizarAttemptsMinimoAUno() {
        var stats = new StageExecutionStats("S", true, 1L, 0, false, "");

        assertEquals(1, stats.attempts());
    }

    @Test
    void constructor_deberiaNormalizarErrorNuloAVacio() {
        var stats = new StageExecutionStats("S", true, 1L, 1, false, null);

        assertEquals("", stats.errorMessage());
    }

    @Test
    void retries_deberiaRestarUnoAIntentos() {
        assertEquals(0, StageExecutionStats.success("S", 1L, 1).retries());
        assertEquals(2, StageExecutionStats.failure("S", 1L, 3, "x").retries());
        assertEquals(0, new StageExecutionStats("S", true, 1L, 0, false, "").retries());
    }

    @Test
    void equals_hashCode_toString_deberianComportarseComoRecord() {
        var a = StageExecutionStats.success("S", 1L, 1);
        var b = StageExecutionStats.success("S", 1L, 1);
        var c = StageExecutionStats.success("T", 1L, 1);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertTrue(a.toString().contains("S"));
    }
}
