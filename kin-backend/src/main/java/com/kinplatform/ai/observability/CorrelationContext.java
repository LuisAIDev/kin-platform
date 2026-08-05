package com.kinplatform.ai.observability;

import java.util.UUID;

/**
 * Contexto de correlación (Fase 7 — observabilidad). Vive en infraestructura,
 * NUNCA en el dominio. Porta {@code correlationId}, {@code requestId} y
 * {@code traceId} por request (ThreadLocal) para logs y métricas.
 *
 * <p>Los IDs generados aquí (UUID) son de trazabilidad, no decisiones de
 * dominio: el dominio permanece 100 % determinista.</p>
 */
public final class CorrelationContext {

    public static final Correlation UNKNOWN = new Correlation("unknown", "unknown", "unknown");

    private static final ThreadLocal<Correlation> CURRENT = new ThreadLocal<>();

    private CorrelationContext() {
    }

    public record Correlation(String correlationId, String requestId, String traceId) {
        public Correlation {
            correlationId = correlationId == null || correlationId.isBlank() ? "unknown" : correlationId;
            requestId = requestId == null || requestId.isBlank() ? "unknown" : requestId;
            traceId = traceId == null || traceId.isBlank() ? "unknown" : traceId;
        }
    }

    /**
     * Genera e instala un nuevo contexto de correlación para el request actual.
     */
    public static Correlation start() {
        String id = UUID.randomUUID().toString();
        Correlation correlation = new Correlation(id, id, "trace-" + id);
        CURRENT.set(correlation);
        return correlation;
    }

    /**
     * Contexto del request actual; si no existe, genera uno (idempotente).
     */
    public static Correlation current() {
        Correlation correlation = CURRENT.get();
        if (correlation == null) {
            return start();
        }
        return correlation;
    }

    public static void set(Correlation correlation) {
        CURRENT.set(correlation == null ? UNKNOWN : correlation);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
