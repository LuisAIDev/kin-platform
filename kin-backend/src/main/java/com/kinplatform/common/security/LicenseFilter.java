package com.kinplatform.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Order(1)
public class LicenseFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LicenseFilter.class);

    @Value("${kin.licensing.enabled:false}")
    private boolean licensingEnabled;

    @Value("${kin.licensing.product:all}")
    private String product;

    // Rutas compartidas permitidas siempre (incluso con licencia restrictiva)
    private static final List<String> SHARED_PATHS = List.of(
        "/api/v1/health/data-deletion",
        "/api/v1/health/data-export",
        "/api/v1/health/data-rectification",
        "/api/v1/health/consents",
        "/api/v1/admin/health/email",
        "/api/v1/subscriptions",
        "/api/v1/stripe",
        "/api/v1/auth",
        "/api/v1/auth/test"
    );

    // Prefijos exclusivos de PLATFORM (bloquear cuando product=medical)
    private static final List<String> PLATFORM_PREFIXES = List.of(
        "/api/v1/projects/",
        "/api/v1/admin/users",
        "/api/v1/admin/outbox/",
        "/api/v1/admin/security/",
        "/api/v1/test",
        "/api/v1/knowledge",
        "/api/v1/enterprise/",
        "/api/v1/admin/outbox/",
        "/api/v1/admin/security/",
        "/api/v1/admin/users"
    );

    // Prefijos exclusivos de MEDICAL (bloquear cuando product=platform)
    // NOTA: Excluye rutas compartidas listadas arriba
    private static final List<String> MEDICAL_PREFIXES = List.of(
        "/api/v1/health/",
        "/api/v1/billing/",
        "/api/v1/catalogs",
        "/api/v1/institutional/",
        "/api/v1/admin/health/",
        "/api/v1/admin/licensing",
        "/api/v1/wompi"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Modo SaaS: todo permitido
        if (!licensingEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();

        // product=all → todo permitido
        if ("all".equalsIgnoreCase(product)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Rutas compartidas siempre permitidas
        if (SHARED_PATHS.stream().anyMatch(uri::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        // product=medical → bloquear rutas exclusivas de PLATFORM
        if ("medical".equalsIgnoreCase(product)) {
            if (PLATFORM_PREFIXES.stream().anyMatch(uri::startsWith)) {
                log.warn("Acceso bloqueado por licencia (medical): {}", uri);
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Este módulo no está habilitado en su licencia");
                return;
            }
        }

        // product=platform → bloquear rutas exclusivas de MEDICAL
        if ("platform".equalsIgnoreCase(product)) {
            if (MEDICAL_PREFIXES.stream().anyMatch(uri::startsWith)) {
                // Excepciones: rutas compartidas bajo prefijos médicos
                if ("/api/v1/admin/health/email".equals(uri) ||
                    "/api/v1/health/data-deletion".equals(uri) ||
                    "/api/v1/health/data-export".equals(uri) ||
                    "/api/v1/health/data-rectification".equals(uri) ||
                    "/api/v1/health/consents".equals(uri)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                log.warn("Acceso bloqueado por licencia (platform): {}", uri);
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Este módulo no está habilitado en su licencia");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}