package com.kinplatform.kin.enterprise.application;

import java.util.UUID;

/**
 * Puerto de solicitud de generación del proyecto empresarial (Fase 10,
 * Milestone 2F).
 *
 * <p>Contrato mínimo que la capa de conversación consume como punto de
 * emisión del evento {@code EnterpriseProjectRequested} cuando el pipeline
 * completa {@code REPORT}: el dominio de conversación no conoce la
 * infraestructura empresarial, solo invoca {@link #request(UUID)} y la
 * implementación ({@link DefaultEnterpriseProjectTrigger}) resuelve la versión
 * y publica el evento en el {@code DomainEventBus} existente. El listener de
 * la capa de aplicación captura el evento y delega la generación de forma
 * asíncrona.</p>
 */
@FunctionalInterface
public interface EnterpriseProjectTrigger {

    /**
     * Solicita la generación del proyecto empresarial tras completar un
     * {@code REPORT}.
     *
     * <p>Operación no bloqueante e idempotente: si ya existe una generación en
     * vuelo ({@code REQUESTED}/{@code RUNNING}) no publica ningún evento.</p>
     *
     * @param projectId identificador del proyecto de KIN origen
     * @throws IllegalArgumentException si {@code projectId} es {@code null}
     */
    void request(UUID projectId);
}
