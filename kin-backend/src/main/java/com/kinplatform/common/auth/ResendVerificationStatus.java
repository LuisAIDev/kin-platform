package com.kinplatform.common.auth;

/**
 * Resultado del reenvío del correo de verificación.
 *
 * <p>Permite a la UI mostrar el estado REAL sin afirmar envío cuando no lo hubo.
 * Mantiene HTTP 200 (anti-enumeración de código de estado); NO_ACCOUNT vs SENT
 * revela existencia de la cuenta, decisión de producto aprobada para que la UI
 * no mienta.</p>
 */
public enum ResendVerificationStatus {
    /** Se generó un token nuevo y el correo fue aceptado por el proveedor SMTP. */
    SENT,
    /** La cuenta existe y ya está verificada: no se reenvía. */
    ALREADY_VERIFIED,
    /** La cuenta existe, no verificada, pero se está dentro del cooldown de reenvío. */
    COOLDOWN,
    /** No existe una cuenta asociada a ese correo. */
    NO_ACCOUNT
}

