package com.kinplatform.kin.pipeline.resilience;

/**
 * Política determinista de resiliencia por stage (ADR-017, Etapa E2).
 *
 * <p>Define cuántos reintentos máximos tiene un stage, qué acción tomar ante un
 * fallo ({@code FAIL}/{@code RETRY}/{@code SKIP}) y el timeout en milisegundos.
 * Inmutable y 100 % Java: nunca consulta al LLM ni depende de infraestructura.</p>
 */
public record StagePolicy(
    String stageName,
    int maxRetries,
    FailureAction onFailure,
    long timeoutMillis
) {

    public static final long DEFAULT_TIMEOUT_MILLIS = 5_000L;
    public static final int DEFAULT_MAX_RETRIES = 0;

    /**
     * Acción determinista a tomar cuando un stage falla.
     */
    public enum FailureAction {
        FAIL,
        RETRY,
        SKIP
    }

    public StagePolicy {
        if (stageName == null || stageName.isBlank()) {
            throw new IllegalArgumentException("stageName no puede ser null o vacío");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries no puede ser negativo");
        }
        onFailure = onFailure == null ? FailureAction.FAIL : onFailure;
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis debe ser positivo");
        }
    }

    /**
     * Política por defecto: sin reintentos, fallar ante cualquier error.
     */
    public static StagePolicy failFast(String stageName) {
        return new StagePolicy(stageName, DEFAULT_MAX_RETRIES, FailureAction.FAIL, DEFAULT_TIMEOUT_MILLIS);
    }

    /**
     * Política con reintentos explícitos ante fallo.
     */
    public static StagePolicy retry(String stageName, int maxRetries, long timeoutMillis) {
        return new StagePolicy(stageName, maxRetries, FailureAction.RETRY, timeoutMillis);
    }

    /**
     * Política que omite el stage ante fallo (continúa el pipeline).
     */
    public static StagePolicy skipOnFailure(String stageName) {
        return new StagePolicy(stageName, DEFAULT_MAX_RETRIES, FailureAction.SKIP, DEFAULT_TIMEOUT_MILLIS);
    }

    /**
     * {@code true} si los reintentos del stage se agotaron en el intento dado
     * (intentos 1-based): con {@code maxRetries} se permiten
     * {@code maxRetries + 1} intentos en total (el inicial más los reintentos).
     */
    public boolean retriesExhausted(int attempts) {
        return attempts > maxRetries + 1;
    }
}
