package com.kinplatform.kin.enterprise.events;

import com.kinplatform.kin.event.DomainEvent;

import java.util.UUID;

/**
 * Evento de dominio: fallo en la generación del proyecto empresarial.
 *
 * <p>Se emitirá cuando el {@code EnterpriseGenerationOrchestrator} falle al
 * generar una versión del proyecto empresarial (Fase 10), con el motivo del
 * fallo para diagnóstico y reintentos idempotentes. El Milestone 1 solo define
 * el contrato; la emisión se implementará en milestones posteriores.</p>
 *
 * @param projectId identificador del proyecto de KIN origen
 * @param reason    motivo legible del fallo
 */
public record EnterpriseProjectFailed(UUID projectId, String reason) implements DomainEvent {

    @Override
    public String type() {
        return "ENTERPRISE_PROJECT_FAILED";
    }

    @Override
    public Object aggregateId() {
        return projectId;
    }
}
