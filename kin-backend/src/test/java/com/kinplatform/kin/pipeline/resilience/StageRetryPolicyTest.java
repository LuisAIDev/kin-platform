package com.kinplatform.kin.pipeline.resilience;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StageRetryPolicyTest {

    @Test
    void none_deberiaDeshabilitarElReintento() {
        var policy = StageRetryPolicy.none();

        assertEquals(0, policy.maxRetries());
        assertEquals(0L, policy.baseDelayMillis());
        assertEquals(StageRetryPolicy.BackoffStrategy.NONE, policy.backoff());
        assertTrue(policy.eligibleStages().isEmpty());
        assertFalse(policy.isEligible("Cualquier"));
    }

    @Test
    void fixed_deberiaConfigurarBackoffFijo() {
        var policy = StageRetryPolicy.fixed(3, 500L, Set.of("Conocimiento", "Enriquecimiento"));

        assertEquals(3, policy.maxRetries());
        assertEquals(500L, policy.baseDelayMillis());
        assertEquals(StageRetryPolicy.BackoffStrategy.FIXED, policy.backoff());
        assertEquals(Set.of("Conocimiento", "Enriquecimiento"), policy.eligibleStages());
    }

    @Test
    void exponential_deberiaConfigurarBackoffExponencial() {
        var policy = StageRetryPolicy.exponential(3, 100L, Set.of("S"));

        assertEquals(StageRetryPolicy.BackoffStrategy.EXPONENTIAL, policy.backoff());
        assertTrue(policy.isEligible("S"));
    }

    @Test
    void constructor_deberiaRechazarMaxRetriesNegativo() {
        assertThrows(IllegalArgumentException.class,
            () -> new StageRetryPolicy(-1, 0, StageRetryPolicy.BackoffStrategy.FIXED, Set.of("S")));
    }

    @Test
    void constructor_deberiaRechazarBaseDelayNegativo() {
        assertThrows(IllegalArgumentException.class,
            () -> new StageRetryPolicy(1, -1, StageRetryPolicy.BackoffStrategy.FIXED, Set.of("S")));
    }

    @Test
    void constructor_deberiaNormalizarBackoffNuloANone() {
        var policy = new StageRetryPolicy(1, 0, null, Set.of("S"));

        assertEquals(StageRetryPolicy.BackoffStrategy.NONE, policy.backoff());
    }

    @Test
    void constructor_deberiaNormalizarElegiblesNulosAVacio() {
        var policy = new StageRetryPolicy(1, 0, StageRetryPolicy.BackoffStrategy.FIXED, null);

        assertTrue(policy.eligibleStages().isEmpty());
        assertFalse(policy.isEligible("S"));
    }

    @Test
    void isEligible_deberiaSerFalso_cuandoElStageNoEstaListado() {
        var policy = StageRetryPolicy.fixed(1, 0, Set.of("S1"));

        assertTrue(policy.isEligible("S1"));
        assertFalse(policy.isEligible("S2"));
    }

    @Test
    void delayForAttempt_deberiaDevolverCero_paraIntentosInvalidos() {
        var policy = StageRetryPolicy.fixed(3, 500L, Set.of("S"));

        assertEquals(0L, policy.delayForAttempt(0));
        assertEquals(0L, policy.delayForAttempt(1));
    }

    @Test
    void delayForAttempt_fijo_deberiaDevolverLaBase() {
        var policy = StageRetryPolicy.fixed(3, 500L, Set.of("S"));

        assertEquals(500L, policy.delayForAttempt(2));
        assertEquals(500L, policy.delayForAttempt(3));
    }

    @Test
    void delayForAttempt_exponencial_deberiaDuplicarPorIntento() {
        var policy = StageRetryPolicy.exponential(4, 100L, Set.of("S"));

        assertEquals(100L, policy.delayForAttempt(2));
        assertEquals(200L, policy.delayForAttempt(3));
        assertEquals(400L, policy.delayForAttempt(4));
    }

    @Test
    void delayForAttempt_none_deberiaDevolverCero() {
        var policy = new StageRetryPolicy(2, 500L, StageRetryPolicy.BackoffStrategy.NONE, Set.of("S"));

        assertEquals(0L, policy.delayForAttempt(2));
        assertEquals(0L, policy.delayForAttempt(5));
    }

    @Test
    void equals_hashCode_toString_deberianComportarseComoRecord() {
        var a = StageRetryPolicy.fixed(2, 100L, Set.of("S"));
        var b = StageRetryPolicy.fixed(2, 100L, Set.of("S"));
        var c = StageRetryPolicy.none();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertTrue(a.toString().contains("FIXED"));
    }
}
