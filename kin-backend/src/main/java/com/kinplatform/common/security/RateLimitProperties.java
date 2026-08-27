package com.kinplatform.common.security;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración del rate limiting ({@code app.rate-limit.*}).
 *
 * <p>Límites por defecto por prefijo de ruta. {@code RATE_LIMIT_WHITELIST}
 * (comma-separated) exime a IPs concretas (útil para administradores y
 * entornos de prueba).</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    /** Master switch del filtro. */
    private boolean enabled = true;

    /** Si {@code true}, usa {@code X-Forwarded-For} (solo tras proxy de confianza). */
    private boolean trustProxyHeaders = false;

    /** IPs exentas del rate limiting. */
    private List<String> whitelist = new ArrayList<>();

    /** Límites por prefijo de ruta (el prefijo más específico gana). */
    private Map<String, Limit> limits = defaultLimits();

    private static Map<String, Limit> defaultLimits() {
        Map<String, Limit> m = new LinkedHashMap<>();
        m.put("/auth/login", new Limit(10, Duration.ofMinutes(1)));
        m.put("/auth/resend-verification", new Limit(5, Duration.ofMinutes(1)));
        m.put("/auth/", new Limit(10, Duration.ofMinutes(1)));
        m.put("/health/triage/", new Limit(30, Duration.ofMinutes(1)));
        m.put("/health/differential/", new Limit(30, Duration.ofMinutes(1)));
        m.put("/health/telemedicine/", new Limit(60, Duration.ofMinutes(1)));
        return m;
    }

    /** Máximo de solicitudes en una ventana para un prefijo. */
    @Data
    public static class Limit {
        private int max = 10;
        private Duration window = Duration.ofMinutes(1);

        public Limit() {}

        public Limit(int max, Duration window) {
            this.max = max;
            this.window = window;
        }
    }
}
