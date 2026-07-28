package com.kinplatform.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, RateLimitState> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!path.startsWith(request.getContextPath() + "/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIP(request);
        RateLimitState state = buckets.computeIfAbsent(ip, k -> new RateLimitState());

        synchronized (state) {
            Instant now = Instant.now();
            if (state.windowStart.plus(WINDOW).isBefore(now)) {
                state.windowStart = now;
                state.count = 0;
            }
            state.count++;
            if (state.count > MAX_REQUESTS) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Demasiadas solicitudes. Intenta de nuevo en 1 minuto.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private static String getClientIP(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RateLimitState {
        Instant windowStart = Instant.now();
        int count = 0;
    }
}
