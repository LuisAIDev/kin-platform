package com.kinplatform.kin.pipeline.resilience;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StagePolicyTest {

    @Test
    void failFast_deberiaConfigurarValoresPorDefecto() {
        var policy = StagePolicy.failFast("Analizador");

        assertEquals("Analizador", policy.stageName());
        assertEquals(0, policy.maxRetries());
        assertEquals(StagePolicy.FailureAction.FAIL, policy.onFailure());
        assertEquals(StagePolicy.DEFAULT_TIMEOUT_MILLIS, policy.timeoutMillis());
    }

    @Test
    void retry_deberiaConfigurarReintentosYTimeout() {
        var policy = StagePolicy.retry("Conocimiento", 3, 1_000L);

        assertEquals("Conocimiento", policy.stageName());
        assertEquals(3, policy.maxRetries());
        assertEquals(StagePolicy.FailureAction.RETRY, policy.onFailure());
        assertEquals(1_000L, policy.timeoutMillis());
    }

    @Test
    void skipOnFailure_deberiaConfigurarAccionSkip() {
        var policy = StagePolicy.skipOnFailure("Eventos");

        assertEquals(StagePolicy.FailureAction.SKIP, policy.onFailure());
        assertEquals(0, policy.maxRetries());
    }

    @Test
    void constructor_deberiaRechazarStageNameNulo() {
        assertThrows(IllegalArgumentException.class,
            () -> new StagePolicy(null, 0, StagePolicy.FailureAction.FAIL, 100L));
    }

    @Test
    void constructor_deberiaRechazarStageNameVacio() {
        assertThrows(IllegalArgumentException.class,
            () -> new StagePolicy("  ", 0, StagePolicy.FailureAction.FAIL, 100L));
    }

    @Test
    void constructor_deberiaRechazarMaxRetriesNegativo() {
        assertThrows(IllegalArgumentException.class,
            () -> new StagePolicy("S", -1, StagePolicy.FailureAction.FAIL, 100L));
    }

    @Test
    void constructor_deberiaRechazarTimeoutNoPositivo() {
        assertThrows(IllegalArgumentException.class,
            () -> new StagePolicy("S", 0, StagePolicy.FailureAction.FAIL, 0L));
        assertThrows(IllegalArgumentException.class,
            () -> new StagePolicy("S", 0, StagePolicy.FailureAction.FAIL, -5L));
    }

    @Test
    void constructor_deberiaNormalizarAccionNulaAFail() {
        var policy = new StagePolicy("S", 0, null, 100L);

        assertEquals(StagePolicy.FailureAction.FAIL, policy.onFailure());
    }

    @Test
    void retriesExhausted_deberiaDetectarAgotamiento() {
        var policy = StagePolicy.retry("S", 2, 100L);

        assertFalse(policy.retriesExhausted(1));
        assertFalse(policy.retriesExhausted(2));
        assertFalse(policy.retriesExhausted(3));
        assertTrue(policy.retriesExhausted(4));
    }

    @Test
    void retriesExhausted_sinReintentos_deberiaAgotarseTrasElPrimerIntento() {
        var policy = StagePolicy.failFast("S");

        assertFalse(policy.retriesExhausted(1));
        assertTrue(policy.retriesExhausted(2));
    }

    @Test
    void equals_hashCode_toString_deberianComportarseComoRecord() {
        var a = StagePolicy.failFast("S");
        var b = new StagePolicy("S", 0, StagePolicy.FailureAction.FAIL, StagePolicy.DEFAULT_TIMEOUT_MILLIS);
        var c = StagePolicy.failFast("Otro");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertTrue(a.toString().contains("S"));
        assertTrue(a.toString().contains("FAIL"));
    }
}
