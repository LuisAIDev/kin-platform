package com.kinplatform.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro de rate limiting por IP (fase de producción).
 *
 * <p>Aplica ventanas deslizantes configurables por prefijo de ruta en
 * endpoints críticos: autenticación, triaje, diagnóstico diferencial y
 * telemedicina. Cada prefijo mantiene buckets independientes por IP. Por
 * defecto deshabilitado en tests ({@code app.rate-limit.enabled=false});
 * los límites se configuran en {@code application.yml}.</p>
 *
 * <p>{@code trust-proxy-headers}: solo activar tras un proxy de confianza
 * (Render/Vercel); por defecto se usa la IP del peer directo (no falseable).</p>
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    /** Límites por defecto: auth 5/min, salud 30/min, telemedicina 60/min. */
    private static final Map<String, RateLimitConfig> DEFAULT_LIMITS = Map.of(
            "/auth/", new RateLimitConfig("/auth/", 5, Duration.ofMinutes(1)),
            "/health/triage/", new RateLimitConfig("/health/triage/", 30, Duration.ofMinutes(1)),
            "/health/differential/", new RateLimitConfig("/health/differential/", 30, Duration.ofMinutes(1)),
            "/health/telemedicine/", new RateLimitConfig("/health/telemedicine/", 60, Duration.ofMinutes(1)));

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled = true;

    @Value("${app.rate-limit.trust-proxy-headers:false}")
    private boolean trustProxyHeaders;

    private final Map<String, RateLimitState> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }
        String p = path.startsWith(contextPath) ? path.substring(contextPath.length()) : path;
        RateLimitConfig config = matchConfig(p);

        if (config == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIP(request, trustProxyHeaders);
        RateLimitState state = buckets.computeIfAbsent(ip + "|" + config.prefix(), k -> new RateLimitState());

        synchronized (state) {
            Instant now = Instant.now();
            if (state.windowStart.plus(config.window()).isBefore(now)) {
                state.windowStart = now;
                state.count = 0;
            }
            state.count++;
            if (state.count > config.maxRequests()) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Demasiadas solicitudes. Intenta de nuevo.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private static RateLimitConfig matchConfig(String path) {
        for (Map.Entry<String, RateLimitConfig> entry : DEFAULT_LIMITS.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return new RateLimitConfig(
                        entry.getKey(),
                        entry.getValue().maxRequests(),
                        entry.getValue().window());
            }
        }
        return null;
    }

    private static String getClientIP(HttpServletRequest request, boolean trustProxyHeaders) {
        if (trustProxyHeaders) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /** Configuración de límite y ventana para un prefijo de ruta. */
    public record RateLimitConfig(String prefix, int maxRequests, Duration window) {}

    private static class RateLimitState {
        Instant windowStart = Instant.now();
        int count;
    }
}
