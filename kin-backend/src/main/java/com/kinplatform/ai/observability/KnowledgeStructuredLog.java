package com.kinplatform.ai.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Logging estructurado del Knowledge Engine (Fase 7 — observabilidad).
 * Emite eventos con {@code correlationId}, {@code requestId}, {@code traceId},
 * duración y resultado.
 *
 * <p>Nunca se registran prompts, secretos, tokens, credenciales ni datos
 * personales: solo identificadores, duraciones, conteos y resultados.</p>
 */
public final class KnowledgeStructuredLog {

    private static final Logger LOG = LoggerFactory.getLogger("kin.observability.knowledge");

    private KnowledgeStructuredLog() {
    }

    public static void cycle(long durationMs, String result) {
        CorrelationContext.Correlation correlation = CorrelationContext.current();
        Map<String, Object> fields = base(correlation);
        fields.put("durationMs", durationMs);
        fields.put("result", result);
        withMdc(correlation, () -> LOG.info("knowledge.cycle {}", toJson(fields)));
    }

    public static void event(String type, String result) {
        CorrelationContext.Correlation correlation = CorrelationContext.current();
        Map<String, Object> fields = base(correlation);
        fields.put("type", type);
        fields.put("result", result);
        withMdc(correlation, () -> LOG.info("knowledge.event {}", toJson(fields)));
    }

    public static void error(String reason) {
        CorrelationContext.Correlation correlation = CorrelationContext.current();
        Map<String, Object> fields = base(correlation);
        fields.put("reason", reason);
        withMdc(correlation, () -> LOG.error("knowledge.error {}", toJson(fields)));
    }

    private static Map<String, Object> base(CorrelationContext.Correlation correlation) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("correlationId", correlation.correlationId());
        fields.put("requestId", correlation.requestId());
        fields.put("traceId", correlation.traceId());
        return fields;
    }

    private static void withMdc(CorrelationContext.Correlation correlation, Runnable runnable) {
        MDC.put("correlationId", correlation.correlationId());
        MDC.put("requestId", correlation.requestId());
        MDC.put("traceId", correlation.traceId());
        try {
            runnable.run();
        } finally {
            MDC.remove("correlationId");
            MDC.remove("requestId");
            MDC.remove("traceId");
        }
    }

    private static String toJson(Map<String, Object> fields) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append('"').append(escape(entry.getKey())).append("\": ");
            Object value = entry.getValue();
            if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append('"').append(escape(String.valueOf(value))).append('"');
            }
        }
        return sb.append('}').toString();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
