package com.kinplatform.kin.enterprise.web;

/**
 * Excepción web de recurso no encontrado (Fase 10, Milestone 2I).
 *
 * <p>Se lanza cuando una solicitud de la API Enterprise referencia un recurso
 * inexistente: una versión del proyecto empresarial que no existe, una versión
 * sin documentos o un documento/formato ausente. El
 * {@link EnterpriseApiExceptionHandler} la traduce a HTTP 404 Not Found.</p>
 */
public class EnterpriseNotFoundException extends RuntimeException {

    /**
     * @param message motivo descriptivo del recurso no encontrado
     */
    public EnterpriseNotFoundException(String message) {
        super(message);
    }
}
