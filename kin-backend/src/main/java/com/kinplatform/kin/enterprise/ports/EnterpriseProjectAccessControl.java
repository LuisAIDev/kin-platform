package com.kinplatform.kin.enterprise.ports;

import java.util.UUID;

/**
 * Control de acceso del módulo Enterprise (remediación C1).
 *
 * <p>Puerto de dominio que verifica si un proyecto de KIN pertenece al usuario
 * autenticado. Lo consume la capa web (interceptor) para impedir que un usuario
 * lea, genere, exporte o se suscriba al SSE de proyectos ajenos. La
 * implementación de infraestructura resuelve el {@code Project} y su propietario
 * sin acoplar el dominio al repositorio concreto.</p>
 */
public interface EnterpriseProjectAccessControl {

    /**
     * Indica si el proyecto pertenece al usuario autenticado.
     *
     * @param projectId              identificador del proyecto de KIN origen
     * @param authenticatedUserEmail email del usuario autenticado (del token JWT)
     * @return {@code true} si el proyecto existe y su propietario coincide con el
     *         usuario; {@code false} si el proyecto no existe, el usuario no
     *         existe o el proyecto pertenece a otro usuario
     */
    boolean isOwner(UUID projectId, String authenticatedUserEmail);
}
