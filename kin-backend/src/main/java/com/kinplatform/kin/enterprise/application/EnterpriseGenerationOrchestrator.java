package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;

import java.util.UUID;

/**
 * Fachada de generación del proyecto empresarial (Fase 10).
 *
 * <p>Caso de uso de aplicación que orquesta la generación completa del
 * proyecto empresarial: construcción de la entrada, ejecución de los motores
 * deterministas, narrativa (via {@code AIResponder}), ensamblado de
 * documentos, persistencia y eventos de dominio.</p>
 *
 * <p>El Milestone 1 define únicamente la firma de la fachada sin lógica: la
 * implementación del flujo de generación se realizará en el Milestone 2.
 * Invocar este método actualmente lanza {@link UnsupportedOperationException}.</p>
 *
 * @param projectId identificador del proyecto de KIN origen
 * @return el proyecto empresarial generado
 * @throws UnsupportedOperationException generación aún no implementada
 */
public final class EnterpriseGenerationOrchestrator {

    public EnterpriseProject generate(UUID projectId) {
        throw new UnsupportedOperationException("Enterprise generation not implemented yet");
        // TODO Phase 10: construir entrada, ejecutar motores, ensamblar documentos,
        //  persistir aggregate y emitir eventos de dominio.
    }
}
