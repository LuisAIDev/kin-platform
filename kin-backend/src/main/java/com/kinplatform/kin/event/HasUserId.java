package com.kinplatform.kin.event;

/**
 * Interfaz opcional para eventos que llevan un {@code userId}.
 * Permite enriquecer la metadata del Outbox con el ID de usuario.
 */
public interface HasUserId {
    /**
     * @return ID del usuario que originó el evento (puede ser {@code null} si no aplica)
     */
    java.util.UUID userId();
}