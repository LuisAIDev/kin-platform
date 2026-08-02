package com.kinplatform.kin.pipeline.resilience;

import java.util.Set;

/**
 * Estrategia de reintento determinista (ADR-017, Etapa E2).
 *
 * <p>Define el número máximo de reintentos, la política de backoff
 * ({@code NONE}/{@code FIXED}/{@code EXPONENTIAL}) y el conjunto de stages
 * elegibles (seguros/idempotentes). El retry se limita a los stages listados:
 * un conjunto vacío deshabilita el reintento (fail-fast, patrón ADR-017).</p>
 */
public record StageRetryPolicy(
    int maxRetries,
    long baseDelayMillis,
    BackoffStrategy backoff,
    Set<String> eligibleStages
) {

    public enum BackoffStrategy {
        NONE,
        FIXED,
        EXPONENTIAL
    }

    public StageRetryPolicy {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries no puede ser negativo");
        }
        if (baseDelayMillis < 0) {
            throw new IllegalArgumentException("baseDelayMillis no puede ser negativo");
        }
        backoff = backoff == null ? BackoffStrategy.NONE : backoff;
        eligibleStages = (eligibleStages == null || eligibleStages.isEmpty())
            ? Set.of()
            : Set.copyOf(eligibleStages);
    }

    /**
     * Política de reintento deshabilitada (fail-fast).
     */
    public static StageRetryPolicy none() {
        return new StageRetryPolicy(0, 0, BackoffStrategy.NONE, Set.of());
    }

    public static StageRetryPolicy fixed(int maxRetries, long baseDelayMillis, Set<String> eligibleStages) {
        return new StageRetryPolicy(maxRetries, baseDelayMillis, BackoffStrategy.FIXED, eligibleStages);
    }

    public static StageRetryPolicy exponential(int maxRetries, long baseDelayMillis, Set<String> eligibleStages) {
        return new StageRetryPolicy(maxRetries, baseDelayMillis, BackoffStrategy.EXPONENTIAL, eligibleStages);
    }

    /**
     * {@code true} solo si el stage está explícitamente listado como elegible.
     */
    public boolean isEligible(String stageName) {
        return eligibleStages.contains(stageName);
    }

    /**
     * Espera determinista antes del reintento del intento dado (1-based).
     */
    public long delayForAttempt(int attempt) {
        if (attempt <= 1) {
            return 0;
        }
        switch (backoff) {
            case FIXED:
                return baseDelayMillis;
            case EXPONENTIAL:
                return baseDelayMillis * (1L << Math.min(attempt - 2, 30));
            case NONE:
            default:
                return 0;
        }
    }
}
