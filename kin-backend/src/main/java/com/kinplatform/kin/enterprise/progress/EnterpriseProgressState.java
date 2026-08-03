package com.kinplatform.kin.enterprise.progress;

/**
 * Estados del ciclo de generación del proyecto empresarial (Fase 10,
 * Milestone 2J).
 *
 * <p>Modela la secuencia de progreso que se publica vía Server Sent Events
 * (SSE): la generación se solicita ({@code REQUESTED}), se ejecuta
 * ({@code RUNNING}), produce cada documento ({@code DOCUMENT_GENERATED}) y
 * termina en {@code COMPLETED} o {@code FAILED}. Los estados terminales
 * permiten al consumidor SSE detener la reconexión automática.</p>
 */
public enum EnterpriseProgressState {

    REQUESTED,
    RUNNING,
    DOCUMENT_GENERATED,
    COMPLETED,
    FAILED;

    /**
     * Indica si el estado es terminal ({@code COMPLETED} o {@code FAILED}).
     *
     * @return {@code true} si la generación ha finalizado
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
