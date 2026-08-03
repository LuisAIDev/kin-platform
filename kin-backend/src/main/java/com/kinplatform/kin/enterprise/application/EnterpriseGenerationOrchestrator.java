package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Fachada de generación del proyecto empresarial (Fase 10, Milestone 2E).
 *
 * <p>Caso de uso de aplicación que orquesta la generación completa del
 * proyecto empresarial: construcción de la entrada, ejecución de los motores
 * deterministas, ensamblado de documentos, persistencia y eventos de dominio.
 * Delega la lógica en {@link EnterpriseGenerationService}; esta clase es el
 * punto de entrada público del flujo y la que consumirá el adapter REST del
 * Milestone posterior.</p>
 *
 * <p>El Milestone 1 definió únicamente la firma {@link #generate(UUID)} sin
 * lógica; a partir del Milestone 2E el flujo completo se expone mediante la
 * solicitud tipada {@link EnterpriseGenerationRequest} (bloqueante y asíncrona)
 * que porta los resultados del pipeline que consumen los ocho motores. La
 * firma histórica se conserva por compatibilidad binaria y lanza
 * {@link UnsupportedOperationException}: sin los datos del pipeline no puede
 * ejecutar una generación.</p>
 */
public final class EnterpriseGenerationOrchestrator {

    private final EnterpriseGenerationService service;

    /**
     * @param service servicio de generación al que delega el flujo (obligatorio)
     */
    public EnterpriseGenerationOrchestrator(EnterpriseGenerationService service) {
        if (service == null) {
            throw new IllegalArgumentException("El servicio de generación no puede ser null.");
        }
        this.service = service;
    }

    /**
     * Genera el proyecto empresarial de forma bloqueante a partir de la
     * solicitud tipada con los resultados del pipeline.
     *
     * @param request solicitud de generación (obligatoria)
     * @return el aggregate persistido
     */
    public EnterpriseProject generate(EnterpriseGenerationRequest request) {
        return service.generate(request);
    }

    /**
     * Genera el proyecto empresarial de forma asíncrona.
     *
     * @param request solicitud de generación (obligatoria)
     * @return futuro que completa con el aggregate persistido
     */
    public CompletableFuture<EnterpriseProject> generateAsync(EnterpriseGenerationRequest request) {
        return service.generateAsync(request);
    }

    /**
     * Firma histórica del Milestone 1, conservada por compatibilidad binaria.
     *
     * <p>Sin la solicitud tipada (contexto y resultados del pipeline) no es
     * posible ejecutar la generación: utilice
     * {@link #generate(EnterpriseGenerationRequest)} o
     * {@link #generateAsync(EnterpriseGenerationRequest)}.</p>
     *
     * @param projectId identificador del proyecto de KIN origen
     * @return nunca devuelve (siempre lanza)
     * @throws UnsupportedOperationException generación sin solicitud tipada
     */
    public EnterpriseProject generate(UUID projectId) {
        throw new UnsupportedOperationException(
            "La generación requiere una EnterpriseGenerationRequest con los resultados "
            + "del pipeline (Milestone 2E); utilice generate(EnterpriseGenerationRequest).");
    }
}
