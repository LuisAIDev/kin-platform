package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.enterprise.ports.EnterpriseProjectAccessControl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Interceptor de autorización del módulo Enterprise (remediación C1).
 *
 * <p>Impide el acceso (IDOR) a recursos Enterprise de proyectos ajenos: verifica
 * que el {@code projectId} de la ruta {@code /enterprise/{projectId}/...}
 * pertenezca al usuario autenticado. Si no es dueño (o el proyecto/usuario no
 * existe) lanza {@link EnterpriseNotFoundException}, que el
 * {@link EnterpriseApiExceptionHandler} traduce a HTTP 404 (indistinguible de
 * "no existe", evitando filtrar existencia). Se registra exclusivamente para
 * {@code /enterprise/**}.</p>
 */
public class EnterpriseOwnershipInterceptor implements HandlerInterceptor {

    private static final String ENTERPRISE_PREFIX = "/enterprise/";

    private final EnterpriseProjectAccessControl accessControl;

    /**
     * @param accessControl puerto de control de acceso Enterprise (obligatorio)
     */
    public EnterpriseOwnershipInterceptor(EnterpriseProjectAccessControl accessControl) {
        if (accessControl == null) {
            throw new IllegalArgumentException("accessControl no puede ser null");
        }
        this.accessControl = accessControl;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        UUID projectId = extractProjectId(request);
        if (projectId == null) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication != null ? authentication.getName() : null;
        if (email == null || !accessControl.isOwner(projectId, email)) {
            throw new EnterpriseNotFoundException(
                "El proyecto empresarial no existe para el usuario autenticado.");
        }
        return true;
    }

    /**
     * Extrae el {@code projectId} (UUID) de la ruta {@code /enterprise/{id}/...}
     * a partir de la URI (sin el context-path). Devuelve {@code null} si la ruta
     * no es Enterprise o el segmento no es un UUID válido (el controller lo
     * tratará como 400/404).
     */
    private UUID extractProjectId(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        if (!uri.startsWith(ENTERPRISE_PREFIX)) {
            return null;
        }
        String rest = uri.substring(ENTERPRISE_PREFIX.length());
        int slash = rest.indexOf('/');
        String raw = slash >= 0 ? rest.substring(0, slash) : rest;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
