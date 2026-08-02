package com.kinplatform.kin.pipeline.resilience;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StageTimeoutConfigTest {

    @Test
    void defaultTimeout_deberiaConfigurarValoresPorDefecto() {
        var config = StageTimeoutConfig.defaultTimeout(5_000L);

        assertEquals(5_000L, config.defaultTimeoutMillis());
        assertEquals(StageTimeoutConfig.TimeoutAction.FAIL, config.onTimeout());
        assertTrue(config.timeoutByStageMillis().isEmpty());
    }

    @Test
    void constructor_deberiaRechazarDefaultTimeoutNoPositivo() {
        assertThrows(IllegalArgumentException.class, () -> new StageTimeoutConfig(Map.of(), 0L, null));
        assertThrows(IllegalArgumentException.class, () -> new StageTimeoutConfig(Map.of(), -1L, null));
    }

    @Test
    void constructor_deberiaRechazarStageKeyNuloOVacio() {
        assertThrows(IllegalArgumentException.class,
            () -> new StageTimeoutConfig(Map.of(" ", 100L), 1_000L, null));
    }

    @Test
    void constructor_deberiaRechazarTimeoutEspecificoNoPositivo() {
        assertThrows(IllegalArgumentException.class,
            () -> new StageTimeoutConfig(Map.of("S", 0L), 1_000L, null));
        assertThrows(IllegalArgumentException.class,
            () -> new StageTimeoutConfig(Map.of("S", -5L), 1_000L, null));
    }

    @Test
    void constructor_deberiaNormalizarMapaNuloAVacio() {
        var config = new StageTimeoutConfig(null, 1_000L, null);

        assertTrue(config.timeoutByStageMillis().isEmpty());
    }

    @Test
    void constructor_deberiaNormalizarAccionNulaAFail() {
        var config = new StageTimeoutConfig(Map.of("S", 100L), 1_000L, null);

        assertEquals(StageTimeoutConfig.TimeoutAction.FAIL, config.onTimeout());
    }

    @Test
    void timeoutMillisFor_deberiaPriorizarElEspecifico() {
        var config = new StageTimeoutConfig(Map.of("S1", 100L, "S2", 200L), 1_000L,
            StageTimeoutConfig.TimeoutAction.SKIP);

        assertEquals(100L, config.timeoutMillisFor("S1"));
        assertEquals(200L, config.timeoutMillisFor("S2"));
        assertEquals(1_000L, config.timeoutMillisFor("S3"));
    }

    @Test
    void equals_hashCode_toString_deberianComportarseComoRecord() {
        var a = new StageTimeoutConfig(Map.of("S", 100L), 1_000L, StageTimeoutConfig.TimeoutAction.SKIP);
        var b = new StageTimeoutConfig(Map.of("S", 100L), 1_000L, StageTimeoutConfig.TimeoutAction.SKIP);
        var c = StageTimeoutConfig.defaultTimeout(1_000L);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertTrue(a.toString().contains("SKIP"));
    }
}
