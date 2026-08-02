package com.kinplatform.kin.pipeline.resilience;

import java.util.concurrent.TimeoutException;

/**
 * Clasificador determinista de errores del pipeline (ADR-017, Etapa E2).
 *
 * <p>Utilidad sin estado y sin infraestructura: construye la
 * {@link PipelineExecutionException} apropiada y detecta timeouts recorriendo
 * la cadena de causas. Decisión 100 % Java.</p>
 */
public final class PipelineErrorHandler {

    private static final int MAX_CAUSE_DEPTH = 10;

    private PipelineErrorHandler() {
    }

    /**
     * Clasifica un fallo y construye la excepción de dominio correspondiente.
     *
     * @param stageName nombre del stage que falló
     * @param error     causa raíz del fallo
     * @param timedOut  {@code true} si el fallo fue por timeout
     * @param attempts  intentos totales realizados (1-based)
     */
    public static PipelineExecutionException classify(String stageName, Throwable error, boolean timedOut, int attempts) {
        PipelineExecutionException.FailureKind kind = kindOf(error, timedOut);
        return new PipelineExecutionException(stageName, kind, error);
    }

    /**
     * {@code true} si la causa (o cualquier causa anidada) es un
     * {@link TimeoutException}.
     */
    public static boolean isTimeout(Throwable error) {
        Throwable current = error;
        int depth = 0;
        while (current != null && depth < MAX_CAUSE_DEPTH) {
            if (current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
            depth++;
        }
        return false;
    }

    /**
     * Determina la clase de fallo: timeout si {@code timedOut} o si la causa es
     * un {@link TimeoutException}; si no, {@code UNEXPECTED}.
     */
    public static PipelineExecutionException.FailureKind kindOf(Throwable error, boolean timedOut) {
        if (timedOut || isTimeout(error)) {
            return PipelineExecutionException.FailureKind.TIMEOUT;
        }
        return PipelineExecutionException.FailureKind.UNEXPECTED;
    }
}
