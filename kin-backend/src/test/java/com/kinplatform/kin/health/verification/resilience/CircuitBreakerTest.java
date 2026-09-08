package com.kinplatform.kin.health.verification.resilience;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Tests del Circuit Breaker usado para las llamadas a la OMS (ADR-041).
 */
class CircuitBreakerTest {

    @Test
    void execute_conExito_devuelveResultado() {
        CircuitBreaker breaker = new CircuitBreaker("who", 3, 1000);
        String result = breaker.execute(() -> "ok", "fallback");
        assertEquals("ok", result);
        assertEquals(CircuitBreaker.State.CLOSED, breaker.state());
    }

    @Test
    void trasTresFallos_abreElCircuito_yDevuelveFallback() {
        CircuitBreaker breaker = new CircuitBreaker("who", 3, 30_000);
        for (int i = 0; i < 3; i++) {
            String result = breaker.execute(() -> {
                throw new IllegalStateException("boom");
            }, "fallback");
            assertEquals("fallback", result);
        }
        assertTrue(breaker.isOpen(), "el breaker debería estar OPEN tras 3 fallos");
    }

    @Test
    void cuandoEstaAbierto_noInvocaLaAccion() {
        CircuitBreaker breaker = new CircuitBreaker("who", 1, 30_000);
        breaker.recordFailure();
        assertTrue(breaker.isOpen());

        AtomicInteger calls = new AtomicInteger();
        String result = breaker.execute(() -> {
            calls.incrementAndGet();
            return "ok";
        }, "fallback");

        assertEquals("fallback", result);
        assertEquals(0, calls.get(), "no debe invocar la acción mientras está abierto");
    }

    @Test
    void trasElTimeout_elExitoEnHalfOpenCierra() throws InterruptedException {
        CircuitBreaker breaker = new CircuitBreaker("who", 1, 40);
        breaker.recordFailure();
        assertTrue(breaker.isOpen());

        Thread.sleep(80);
        String result = breaker.execute(() -> "ok", "fallback");

        assertEquals("ok", result);
        assertEquals(CircuitBreaker.State.CLOSED, breaker.state());
    }

    @Test
    void falloEnHalfOpen_vuelveAbrir() throws InterruptedException {
        CircuitBreaker breaker = new CircuitBreaker("who", 1, 40);
        breaker.recordFailure();
        Thread.sleep(80);

        String result = breaker.execute(() -> {
            throw new IllegalStateException("still down");
        }, "fallback");

        assertEquals("fallback", result);
        assertTrue(breaker.isOpen());
    }

    @Test
    void reset_devuelveAClosed() {
        CircuitBreaker breaker = new CircuitBreaker("who", 1, 30_000);
        breaker.recordFailure();
        assertTrue(breaker.isOpen());

        breaker.reset();
        assertFalse(breaker.isOpen());
        assertEquals(CircuitBreaker.State.CLOSED, breaker.state());
    }

    @Test
    void parametrosInvalidos_lanzan() {
        assertThrows(IllegalArgumentException.class, () -> new CircuitBreaker("", 1, 100));
        assertThrows(IllegalArgumentException.class, () -> new CircuitBreaker("who", 0, 100));
        assertThrows(IllegalArgumentException.class, () -> new CircuitBreaker("who", 1, 0));
    }
}
