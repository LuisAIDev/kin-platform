package com.kinplatform.kin.pipeline.resilience;

import java.util.Locale;

/**
 * Excepción de dominio que clasifica un fallo del pipeline (ADR-017, Etapa E2).
 *
 * <p>Transporta el stage que falló y la clase de fallo
 * ({@code TIMEOUT}/{@code RETRY_EXHAUSTED}/{@code UNEXPECTED}) para que el
 * pipeline (E3) decida la estrategia sin inspeccionar causas ajenas.</p>
 */
public class PipelineExecutionException extends RuntimeException {

    public enum FailureKind {
        TIMEOUT,
        RETRY_EXHAUSTED,
        UNEXPECTED
    }

    private final String stageName;
    private final FailureKind kind;

    public PipelineExecutionException(String stageName, FailureKind kind, String message, Throwable cause) {
        super(message, cause);
        this.stageName = stageName;
        this.kind = kind == null ? FailureKind.UNEXPECTED : kind;
    }

    public PipelineExecutionException(String stageName, FailureKind kind, Throwable cause) {
        this(stageName, kind, messageFor(stageName, kind), cause);
    }

    public PipelineExecutionException(String stageName, FailureKind kind, String message) {
        super(message);
        this.stageName = stageName;
        this.kind = kind == null ? FailureKind.UNEXPECTED : kind;
    }

    private static String messageFor(String stageName, FailureKind kind) {
        return "Fallo del stage '" + stageName + "': "
            + (kind == null ? "desconocido" : kind.name().toLowerCase(Locale.ROOT));
    }

    public String stageName() {
        return stageName;
    }

    public FailureKind kind() {
        return kind;
    }

    public boolean isTimeout() {
        return kind == FailureKind.TIMEOUT;
    }

    public boolean isRetryExhausted() {
        return kind == FailureKind.RETRY_EXHAUSTED;
    }
}
