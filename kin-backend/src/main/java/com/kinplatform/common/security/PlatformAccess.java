package com.kinplatform.common.security;

import java.util.Map;
import java.util.Set;

/**
 * @deprecated Esta clase se mantiene solo como referencia histórica.
 * El aislamiento por plataforma fue removido porque:
 * - Los frontends están completamente separados (kin-platform.com vs kin-platform-medical.com).
 * - Las reglas de negocio (ADR-039/040) permiten capacidades cruzadas.
 * - La seguridad real está en {@code physician_verification_status} y {@code health_data_consent}.
 *
 * <p>Ver: docs/adr/ADR-042_SHARED_BACKEND_MULTI_FRONTEND.md.</p>
 */
@Deprecated
public final class PlatformAccess {

    private static final Map<String, Set<String>> PATH_PLATFORM_MAP = Map.of(
        "/empresas", Set.of("EMPRESAS"),
        "/health", Set.of("SALUD_PERSONAL", "SALUD_PROFESIONAL"),
        "/medical", Set.of("SALUD_PERSONAL", "SALUD_PROFESIONAL")
    );

    private PlatformAccess() {}

    /**
     * Determina si un usuario con la plataforma y rol dados puede acceder a la ruta solicitada.
     *
     * @param platform      plataforma del usuario (EMPRESAS, SALUD_PERSONAL, SALUD_PROFESIONAL).
     * @param role          rol del usuario (FREE, PREMIUM, FACILITADOR, PATIENT, PHYSICIAN, ADMIN).
     * @param requestPath   ruta de la petición.
     * @return {@code true} si el acceso está permitido.
     */
    public static boolean canAccess(String platform, String role, String requestPath) {
        if (requestPath == null) {
            return false;
        }
        if ("ADMIN".equals(role)) {
            return true;
        }
        String normalizedPath = requestPath.startsWith("/") ? requestPath : "/" + requestPath;
        if (isUnrestricted(normalizedPath)) {
            return true;
        }
        if (platform == null) {
            return false;
        }
        return PATH_PLATFORM_MAP.entrySet().stream()
            .filter(entry -> normalizedPath.startsWith(entry.getKey()))
            .anyMatch(entry -> entry.getValue().contains(platform));
    }

    /**
     * Determina si el acceso está permitido sin restricción de plataforma.
     * Rutas como /auth/**, /actuator/**, /pricing-plans/** pasan por aquí.
     */
    public static boolean isUnrestricted(String requestPath) {
        return requestPath == null || PATH_PLATFORM_MAP.keySet().stream()
            .noneMatch(prefix -> requestPath.startsWith(prefix));
    }
}
