package com.kinplatform.kin.enterprise.application;

/**
 * Excepción de dominio de la exportación de documentos del proyecto
 * empresarial (Fase 10, Milestone 2H).
 *
 * <p>Se lanza cuando la exportación no puede completarse: versión del proyecto
 * empresarial inexistente en el repositorio o documento solicitado ausente en
 * la versión. Es una excepción no comprobada: las condiciones de exportación
 * inválidas son errores de datos, no recuperables en el flujo normal.</p>
 */
public class EnterpriseExportException extends RuntimeException {

    /**
     * @param message motivo descriptivo del fallo de exportación
     */
    public EnterpriseExportException(String message) {
        super(message);
    }
}
