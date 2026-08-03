package com.kinplatform.kin.enterprise.progress;

/**
 * Puerto de salida de publicación de progreso (Fase 10, Milestone 2J).
 *
 * <p>Contrato hexagonal que permite a la capa de aplicación publicar
 * {@link EnterpriseProgressEvent} sin conocer el transporte (Server Sent
 * Events, logs, etc.). La implementación ({@code EnterpriseProgressService})
 * se aporta en la capa web y es responsable de distribuir el evento a los
 * clientes suscritos de forma thread-safe.</p>
 */
@FunctionalInterface
public interface EnterpriseProgressSink {

    /**
     * Publica un evento de progreso.
     *
     * @param event evento de progreso a distribuir (obligatorio)
     */
    void publish(EnterpriseProgressEvent event);
}
