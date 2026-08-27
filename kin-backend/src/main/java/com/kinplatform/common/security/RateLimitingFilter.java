package com.kinplatform.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro de rate limiting por IP (fase de producción).
 *
 * <p>Aplica ventanas deslizantes configurables por prefijo de ruta en
 * endpoints críticos: autenticación, triaje, diagnóstico diferencial y
 * telemedicina. Cada prefijo mantiene buckets independientes por IP y se usa
 * el prefijo <b>más específico</b> (p. ej. {@code /auth/login} antes que
 * {@code /auth/}). Config en {@code RateLimitProperties}
 * ({@code app.rate-limit.*}).</p>
 *
 * <p>Las IPs de {@code RATE_LIMIT_WHITELIST} quedan exentas. Los
 * administradores pueden desbloquear una IP con
 * {@code POST /admin/security/rate-limit/reset?ip=...}.</p>
 */
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;

    private final Map<String, RateLimitState> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        String p = path.startsWith(contextPath) ? path.substring(contextPath.length()) : path;
        Map.Entry<String, RateLimitProperties.Limit> match = matchLimit(p);

        if (match == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIP(request, properties.isTrustProxyHeaders());
        if (isWhitelisted(ip)) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitProperties.Limit limit = match.getValue();
        RateLimitState state = buckets.computeIfAbsent(ip + "|" + match.getKey(), k -> new RateLimitState());

        synchronized (state) {
            Instant now = Instant.now();
            if (state.windowStart.plus(limit.getWindow()).isBefore(now)) {
                state.windowStart = now;
                state.count = 0;
            }
            state.count++;
            if (state.count > limit.getMax()) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.setHeader(
                        "Retry-After", String.valueOf(limit.getWindow().toSeconds()));
                response.getWriter()
                        .write("{\"error\":\"Demasiadas solicitudes. Espera un momento antes de "
                                + "intentar de nuevo.\",\"code\":\"RATE_LIMITED\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Map.Entry<String, RateLimitProperties.Limit> matchLimit(String path) {
        Map.Entry<String, RateLimitProperties.Limit> best = null;
        for (Map.Entry<String, RateLimitProperties.Limit> entry :
                properties.getLimits().entrySet()) {
            if (path.startsWith(entry.getKey())
                    && (best == null || entry.getKey().length() > best.getKey().length())) {
                best = entry;
            }
        }
        return best;
    }

    private boolean isWhitelisted(String ip) {
        return properties.getWhitelist() != null && properties.getWhitelist().contains(ip);
    }

    static String getClientIP(HttpServletRequest request, boolean trustProxyHeaders) {
        if (trustProxyHeaders) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /** IP del cliente, con la misma lógica del filtro (usada por el endpoint admin). */
    public String resolveClientIp(HttpServletRequest request) {
        return getClientIP(request, properties.isTrustProxyHeaders());
    }

    /**
     * Elimina los buckets de rate limiting de una IP (todas las rutas). Útil
     * para desbloquear a un usuario legítimo (endpoint ADMIN).
     *
     * @return número de buckets eliminados.
     */
    public int reset(String ip) {
        if (ip == null || ip.isBlank()) {
            return 0;
        }
        String prefix = ip + "|";
        int before = buckets.size();
        buckets.keySet().removeIf(k -> k.startsWith(prefix));
        return before - buckets.size();
    }

    private static class RateLimitState {
        Instant windowStart = Instant.now();
        int count;
    }
}
