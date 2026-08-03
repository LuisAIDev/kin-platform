package com.kinplatform.kin.enterprise.events;

import com.kinplatform.kin.event.DomainEvent;

import java.util.UUID;

/**
 * Evento de dominio: fallo en la generación del proyecto empresarial.
 *
 * <p>Se emitirá cuando el {@code EnterpriseGenerationOrchestrator} falle al
 * generar una versión del proyecto empresarial (Fase 10), con la versión y el
 * motivo del fallo para diagnóstico y reintentos idempotentes. La versión
 * permite correlacionar el fallo con la petición
 * ({@code EnterpriseProjectRequested}) y con la versión concreta que falló. El
 * Milestone 2A solo define el contrato; la emisión se implementará en
 * milestones posteriores.</p>
 *
 * @param projectId identificador del proyecto de KIN origen
 * @param version   versión del proyecto empresarial que falló
 * @param reason    motivo legible del fallo
 */
public record EnterpriseProjectFailed(UUID projectId, int version, String reason) implements DomainEvent {

    @Override
    public String type() {
        return "ENTERPRISE_PROJECT_FAILED";
    }

    @Override
    public Object aggregateId() {
        return projectId;
    }
}
