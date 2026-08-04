package com.kinplatform.kin.enterprise.application;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerta de entrega de los resultados reales del pipeline a la generación del
 * proyecto empresarial (Fase 10, Milestone 3C).
 *
 * <p>El runtime ({@code KinMethod}) publica los resultados del turno
 * {@code REPORT} mediante {@link #store} y el listener de la solicitud de
 * generación ({@code EnterpriseProjectRequestedListener}) los recupera con
 * {@link #consume} para construir la {@code EnterpriseGenerationRequest} con
 * datos reales. Es un canal aditivo e independiente del {@code DomainEventBus}:
 * los eventos de dominio (contrato congelado) no se modifican.</p>
 *
 * <p>Implementación de referencia {@code InMemoryEnterprisePipelineResultStore}
 * (en proceso, correlación por {@code projectId}): en una JVM única la
 * captura y el consumo ocurren en el mismo nodo. Sin resultados disponibles
 * (p. ej. generación manual vía REST) {@link #consume} devuelve vacío y la
 * generación opera en modo offline-first.</p>
 */
public interface EnterprisePipelineResultStore {

    /**
     * Publica los resultados del último turno {@code REPORT} del proyecto.
     *
     * @param results resultados del pipeline (o {@code null} → no operativo)
     */
    void store(EnterpriseTurnResults results);

    /**
     * Recupera y elimina los resultados del proyecto (consumo único).
     *
     * @param projectId identificador del proyecto de KIN origen
     * @return los resultados del turno si existen; vacío en modo offline
     */
    Optional<EnterpriseTurnResults> consume(UUID projectId);
}
