package com.kinplatform.kin.pipeline.resilience;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuración determinista de timeout por stage (ADR-017, Etapa E2).
 *
 * <p>Define un timeout por defecto (ms) y una tabla opcional de timeouts
 * específicos por stage, junto con la acción a tomar ante un timeout
 * ({@code FAIL}/{@code SKIP}/{@code RETRY}). Inmutable y sin infraestructura.</p>
 */
public record StageTimeoutConfig(
    Map<String, Long> timeoutByStageMillis,
    long defaultTimeoutMillis,
    TimeoutAction onTimeout
) {

    public enum TimeoutAction {
        FAIL,
        SKIP,
        RETRY
    }

    public StageTimeoutConfig {
        if (defaultTimeoutMillis <= 0) {
            throw new IllegalArgumentException("defaultTimeoutMillis debe ser positivo");
        }
        onTimeout = onTimeout == null ? TimeoutAction.FAIL : onTimeout;
        if (timeoutByStageMillis == null || timeoutByStageMillis.isEmpty()) {
            timeoutByStageMillis = Map.of();
        } else {
            Map<String, Long> validated = new HashMap<>();
            for (Map.Entry<String, Long> entry : timeoutByStageMillis.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) {
                    throw new IllegalArgumentException("stageName no puede ser null o vacío");
                }
                if (entry.getValue() == null || entry.getValue() <= 0) {
                    throw new IllegalArgumentException("timeout por stage debe ser positivo");
                }
                validated.put(entry.getKey(), entry.getValue());
            }
            timeoutByStageMillis = Collections.unmodifiableMap(validated);
        }
    }

    /**
     * Configuración con timeout por defecto y acción {@code FAIL}.
     */
    public static StageTimeoutConfig defaultTimeout(long defaultTimeoutMillis) {
        return new StageTimeoutConfig(Map.of(), defaultTimeoutMillis, TimeoutAction.FAIL);
    }

    /**
     * Timeout efectivo para un stage: el específico si existe, si no el por defecto.
     */
    public long timeoutMillisFor(String stageName) {
        Long specific = timeoutByStageMillis.get(stageName);
        return specific != null ? specific : defaultTimeoutMillis;
    }
}
