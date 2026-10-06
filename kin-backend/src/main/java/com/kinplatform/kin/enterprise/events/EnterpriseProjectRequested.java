package com.kinplatform.kin.enterprise.events;

import com.kinplatform.common.event.DomainEvent;

import java.util.UUID;

/**
 * Evento de dominio: solicitud de generación del proyecto empresarial.
 *
 * <p>Se emitirá cuando la conversación de un proyecto alcance la decisión
 * {@code REPORT} y el informe de consultoría haya sido generado, solicitando
 * al {@code EnterpriseGenerationOrchestrator} la generación del proyecto
 * empresarial (Fase 10). Porta la versión solicitada para correlacionar la
 * petición con su resultado (versión establecida por el orquestador como
 * {@code siguiente = última + 1}). El Milestone 2A solo define el contrato; la
 * emisión y el consumo se implementarán en milestones posteriores.</p>
 *
 * @param projectId    identificador del proyecto de KIN origen
 * @param version      versión solicitada del proyecto empresarial
 * @param correlationId ID opcional de correlación para idempotencia (PR 4); si no se provee,
 *                      se genera automáticamente en el listener
 */
public record EnterpriseProjectRequested(UUID projectId, int version, String correlationId) implements DomainEvent {

    public EnterpriseProjectRequested(UUID projectId, int version) {
        this(projectId, version, null);
    }

    @Override
    public String type() {
        return "ENTERPRISE_PROJECT_REQUESTED";
    }

    @Override
    public Object aggregateId() {
        return projectId;
    }
}

