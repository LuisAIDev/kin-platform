package com.kinplatform.kin.enterprise.aggregate;

/**
 * Estado del ciclo de vida del proyecto empresarial (aggregate root).
 *
 * <p>Representa la máquina de estados de la generación del proyecto
 * empresarial: solicitado, en generación, completado o fallido. Las
 * transiciones de estado se aplicarán en el Milestone 2 por el
 * {@code EnterpriseGenerationOrchestrator} (el Milestone 1 solo define la
 * estructura, sin lógica de negocio).</p>
 */
public enum GenerationStatus {
    REQUESTED,
    RUNNING,
    COMPLETED,
    FAILED
}
