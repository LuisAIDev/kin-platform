package com.kinplatform.common.ai.usage;

import com.kinplatform.platform.usage.AiReservation;

/**
 * Receptor del uso real de tokens reportado por el proveedor de IA. El
 * proveedor no conoce el usuario; la atribución se resuelve con la reserva
 * vigente en el hilo o, para flujos asíncronos (streaming en otro hilo), con
 * una reserva capturada explícitamente.
 */
public interface AiUsageRecorder {

    /**
     * Registra el uso real contra la reserva vigente en el hilo. Sin reserva
     * vigente no registra nada.
     */
    void recordActual(long inputTokens, long outputTokens);

    /**
     * Registra el uso real contra una reserva explícita (robusto para
     * streaming que completa en otro hilo). Sin reserva no registra nada.
     */
    void recordActual(AiReservation reservation, long inputTokens, long outputTokens);
}


