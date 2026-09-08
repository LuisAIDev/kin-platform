package com.kinplatform.kin.health.verification.port;

/**
 * Error de la capa de verificación con fuentes OMS. Se cuenta como fallo en el
 * Circuit Breaker y degrada el flujo de IA (nunca rompe el chat).
 */
public class WhoHealthDataSourceException extends RuntimeException {

    public WhoHealthDataSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
