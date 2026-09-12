package com.kinplatform.common.security;

import java.util.Map;
import java.util.Set;

/**
 * Predicado central para decidir si un {@link User} con una {@code platform}
 * y un {@code role} dados tienen acceso a un prefijo de ruta específico.
 *
 * <p>Reglas de aislamiento:</p>
 * <ul>
 *   <li>{@code ADMIN} tiene acceso total a TODAS las rutas, independientemente de la plataforma.</li>
 *   <li>{@code /empresas/**} → solo {@code EMPRESAS}</li>
 *   <li>{@code /health/**} y {@code /medical/**} → solo {@code SALUD_PERSONAL} o {@code SALUD_PROFESIONAL}</li>
 *   <li>Rutas sin prefijo mapeado (auth, actuator, pricing-plans, etc.) → siempre permitidas.</li>
 * </ul>
 */
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
     * @param requestPath   ruta de la petición (ej. "/empresas/projects/abc").
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
        // Rutas sin prefijo mapeado son siempre permitidas (auth, actuator, pricing-plans, etc.)
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
