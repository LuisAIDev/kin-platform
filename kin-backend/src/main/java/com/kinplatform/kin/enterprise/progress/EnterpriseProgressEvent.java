package com.kinplatform.kin.enterprise.progress;

import com.kinplatform.kin.enterprise.valueobjects.DocumentType;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de progreso de la generación del proyecto empresarial (Fase 10,
 * Milestone 2J).
 *
 * <p>Representa un paso del ciclo de generación publicado vía Server Sent
 * Events (SSE): identidad de la versión, estado alcanzado, instante, tipo de
 * documento (solo para {@code DOCUMENT_GENERATED}) y mensaje descriptivo
 * opcional. Es un valor inmutable y JSON-serializable.</p>
 *
 * @param projectId    identificador del proyecto de KIN origen
 * @param version      versión del proyecto empresarial
 * @param state        estado alcanzado en el ciclo de generación
 * @param timestamp    instante en que se produjo el evento
 * @param documentType tipo de documento generado (solo {@code DOCUMENT_GENERATED})
 * @param message      mensaje descriptivo opcional
 */
public record EnterpriseProgressEvent(
    UUID projectId,
    int version,
    EnterpriseProgressState state,
    Instant timestamp,
    DocumentType documentType,
    String message
) {

    public EnterpriseProgressEvent {
        if (projectId == null) {
            throw new IllegalArgumentException("'projectId' no puede ser null.");
        }
        if (version < 1) {
            throw new IllegalArgumentException("'version' debe ser mayor o igual a 1.");
        }
        if (state == null) {
            throw new IllegalArgumentException("'state' no puede ser null.");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (state == EnterpriseProgressState.DOCUMENT_GENERATED && documentType == null) {
            throw new IllegalArgumentException(
                "'documentType' es obligatorio para el estado DOCUMENT_GENERATED.");
        }
    }

    /**
     * Crea un evento de progreso con el instante actual.
     *
     * @param projectId    identificador del proyecto de KIN origen
     * @param version      versión del proyecto empresarial
     * @param state        estado alcanzado
     * @param documentType tipo de documento (solo {@code DOCUMENT_GENERATED})
     * @param message      mensaje descriptivo opcional
     * @return evento de progreso
     */
    public static EnterpriseProgressEvent of(UUID projectId, int version,
                                             EnterpriseProgressState state,
                                             DocumentType documentType, String message) {
        return new EnterpriseProgressEvent(projectId, version, state, Instant.now(),
            documentType, message);
    }
}
