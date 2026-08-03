package com.kinplatform.kin.enterprise.events;

import com.kinplatform.kin.event.DomainEvent;

import java.util.UUID;

/**
 * Evento de dominio: proyecto empresarial generado correctamente.
 *
 * <p>Se emitirá cuando el {@code EnterpriseGenerationOrchestrator} complete la
 * generación de una versión del proyecto empresarial (Fase 10). El
 * Milestone 1 solo define el contrato; la emisión se implementará en
 * milestones posteriores.</p>
 *
 * @param projectId identificador del proyecto de KIN origen
 * @param version   versión del proyecto empresarial generada
 */
public record EnterpriseProjectGenerated(UUID projectId, int version) implements DomainEvent {

    @Override
    public String type() {
        return "ENTERPRISE_PROJECT_GENERATED";
    }

    @Override
    public Object aggregateId() {
        return projectId;
    }
}
