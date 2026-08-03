package com.kinplatform.kin.enterprise.events;

import com.kinplatform.kin.event.DomainEvent;

import java.util.UUID;

/**
 * Evento de dominio: solicitud de generación del proyecto empresarial.
 *
 * <p>Se emitirá cuando la conversación de un proyecto alcance la decisión
 * {@code REPORT} y el informe de consultoría haya sido generado, solicitando
 * al {@code EnterpriseGenerationOrchestrator} la generación del proyecto
 * empresarial (Fase 10). El Milestone 1 solo define el contrato; la emisión y
 * el consumo se implementarán en milestones posteriores.</p>
 *
 * @param projectId identificador del proyecto de KIN origen
 */
public record EnterpriseProjectRequested(UUID projectId) implements DomainEvent {

    @Override
    public String type() {
        return "ENTERPRISE_PROJECT_REQUESTED";
    }

    @Override
    public Object aggregateId() {
        return projectId;
    }
}
