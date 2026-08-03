package com.kinplatform.kin.enterprise.aggregate;

/**
 * Excepción de dominio del proyecto empresarial (Fase 10).
 *
 * <p>Se lanza cuando se intenta violar una invariante del aggregate
 * {@link EnterpriseProject}: construcción inválida (identidad, versión,
 * timestamps, documentos), una transición de estado no permitida por la
 * máquina de estados o una operación de dominio incoherente. Es una excepción
 * no comprobada: las violaciones de invariantes son errores de programación o
 * de datos, no condiciones recuperables en el flujo normal.</p>
 */
public class EnterpriseProjectException extends RuntimeException {

    /**
     * @param message motivo descriptivo de la violación de invariante
     */
    public EnterpriseProjectException(String message) {
        super(message);
    }
}
