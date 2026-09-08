package com.kinplatform.kin.health.verification.resilience;

import java.util.function.Supplier;

/**
 * Circuit Breaker genérico (patrón de resiliencia) para llamadas a servicios
 * externos de la OMS (ADR-041).
 *
 * <p>Estados: {@code CLOSED} (llamadas normales) → {@code OPEN} tras
 * {@code failureThreshold} fallos consecutivos → reintento en {@code HALF_OPEN}
 * después de {@code openTimeoutMillis}. Si la llamada de sondeo falla, vuelve a
 * {@code OPEN}. Es seguro para uso concurrente (estado sincronizado) y el
 * resultado degradado (fallback) se devuelve sin lanzar excepciones, de modo que
 * una caída de la OMS nunca tira abajo el chat de salud.</p>
 */
public class CircuitBreaker {

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final String name;
    private final int failureThreshold;
    private final long openTimeoutMillis;
    private final int halfOpenMaxAttempts;

    private volatile State state = State.CLOSED;
    private volatile int consecutiveFailures;
    private volatile int halfOpenAttempts;
    private volatile long openedAtMillis;

    public CircuitBreaker(String name, int failureThreshold, long openTimeoutMillis) {
        this(name, failureThreshold, openTimeoutMillis, 1);
    }

    public CircuitBreaker(String name, int failureThreshold, long openTimeoutMillis, int halfOpenMaxAttempts) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name no puede estar vacío");
        }
        if (failureThreshold <= 0) {
            throw new IllegalArgumentException("failureThreshold debe ser > 0");
        }
        if (openTimeoutMillis <= 0) {
            throw new IllegalArgumentException("openTimeoutMillis debe ser > 0");
        }
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.openTimeoutMillis = openTimeoutMillis;
        this.halfOpenMaxAttempts = Math.max(1, halfOpenMaxAttempts);
    }

    public State state() {
        return state;
    }

    public boolean isOpen() {
        return state == State.OPEN;
    }

    public String name() {
        return name;
    }

    /**
     * Ejecuta {@code action} protegida por el breaker. Si el breaker está
     * {@code OPEN} (o el {@code HALF_OPEN} ya usó sus intentos) devuelve
     * {@code fallback} sin invocar {@code action}. Si {@code action} lanza una
     * excepción, cuenta un fallo y devuelve {@code fallback}.
     */
    public <T> T execute(Supplier<T> action, T fallback) {
        if (!tryAcquire()) {
            return fallback;
        }
        try {
            T result = action.get();
            recordSuccess();
            return result;
        } catch (RuntimeException e) {
            recordFailure();
            return fallback;
        }
    }

    /** Reabre el breaker (reset) — utilidad de tests y de operación. */
    public synchronized void reset() {
        state = State.CLOSED;
        consecutiveFailures = 0;
        halfOpenAttempts = 0;
        openedAtMillis = 0;
    }

    /** Devuelve true si la llamada puede pasar (consumiendo intento en HALF_OPEN). */
    private synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        switch (state) {
            case CLOSED:
                return true;
            case HALF_OPEN:
                if (halfOpenAttempts >= halfOpenMaxAttempts) {
                    return false;
                }
                halfOpenAttempts++;
                return true;
            case OPEN:
            default:
                if (now - openedAtMillis >= openTimeoutMillis) {
                    state = State.HALF_OPEN;
                    halfOpenAttempts = 1;
                    return true;
                }
                return false;
        }
    }

    public synchronized void recordSuccess() {
        consecutiveFailures = 0;
        halfOpenAttempts = 0;
        if (state == State.HALF_OPEN) {
            state = State.CLOSED;
            openedAtMillis = 0;
        }
    }

    public synchronized void recordFailure() {
        consecutiveFailures++;
        halfOpenAttempts = 0;
        if (state == State.HALF_OPEN) {
            state = State.OPEN;
            openedAtMillis = System.currentTimeMillis();
            return;
        }
        if (state == State.CLOSED && consecutiveFailures >= failureThreshold) {
            state = State.OPEN;
            openedAtMillis = System.currentTimeMillis();
        }
    }

    /** Descripción estable para logs y métricas. */
    @Override
    public String toString() {
        return "CircuitBreaker{" + name + ", state=" + state + ", consecutiveFailures="
                + consecutiveFailures + "}";
    }
}
