package com.kinplatform.kin.pipeline.resilience;

/**
 * Estadística inmutable de una única ejecución de stage (ADR-017, Etapa E2).
 *
 * <p>Refleja duración, estado (éxito/fallo), número de intentos, si hubo
 * timeout y el mensaje de error. POJO puro, sin infraestructura.</p>
 */
public record StageExecutionStats(
    String stageName,
    boolean success,
    long durationMillis,
    int attempts,
    boolean timedOut,
    String errorMessage
) {

    public StageExecutionStats {
        if (stageName == null || stageName.isBlank()) {
            throw new IllegalArgumentException("stageName no puede ser null o vacío");
        }
        if (durationMillis < 0) {
            throw new IllegalArgumentException("durationMillis no puede ser negativo");
        }
        attempts = Math.max(1, attempts);
        errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public static StageExecutionStats success(String stageName, long durationMillis, int attempts) {
        return new StageExecutionStats(stageName, true, durationMillis, attempts, false, "");
    }

    public static StageExecutionStats failure(String stageName, long durationMillis, int attempts, String errorMessage) {
        return new StageExecutionStats(stageName, false, durationMillis, attempts, false, errorMessage);
    }

    public static StageExecutionStats timedOut(String stageName, long durationMillis, int attempts) {
        return new StageExecutionStats(stageName, false, durationMillis, attempts, true, "timeout");
    }

    /**
     * Número de reintentos efectuados (intentos totales menos 1).
     */
    public int retries() {
        return Math.max(0, attempts - 1);
    }
}
