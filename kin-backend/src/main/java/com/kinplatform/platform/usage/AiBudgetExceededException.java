package com.kinplatform.platform.usage;

/**
 * El usuario agotó su presupuesto de IA (o una solicitud excede el límite
 * máximo por petición). Se mapea a HTTP 403 con un mensaje amigable.
 */
public class AiBudgetExceededException extends RuntimeException {

    public AiBudgetExceededException(String message) {
        super(message);
    }
}

