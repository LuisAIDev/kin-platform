package com.kinplatform.kin.enterprise.web;

/**
 * Excepción web de solicitud inejecutable (Fase 10, Milestone 2I).
 *
 * <p>Se lanza cuando la solicitud es sintácticamente válida pero no puede
 * procesarse por una condición semántica del estado: p. ej. solicitar la
 * generación de un proyecto sin contexto de conversación disponible. El
 * {@link EnterpriseApiExceptionHandler} la traduce a HTTP 422 Unprocessable
 * Entity.</p>
 */
public class EnterpriseUnprocessableEntityException extends RuntimeException {

    /**
     * @param message motivo descriptivo de la condición inejecutable
     */
    public EnterpriseUnprocessableEntityException(String message) {
        super(message);
    }
}
