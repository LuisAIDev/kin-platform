package com.kinplatform.common.eventbus.port;

import com.kinplatform.common.event.DomainEvent;

/**
 * Puerto de dominio para publicar eventos en el Outbox transaccional.
 *
 * <p>El implementador DEBE escribir en la tabla {@code domain_event_outbox}
 * DENTRO de la misma transacción que el caso de uso que origina el evento.
 * Esto garantiza atomicidad: o se persiste el resultado del caso de uso Y el evento,
 * o no se persiste nada.</p>
 *
 * <p>El relé ({@code OutboxRelay}) leerá posteriormente de la tabla y publicará
 * en el {@link com.kinplatform.common.event.DomainEventBus} real (in-memory,
 * Kafka, etc.).</p>
 */
public interface OutboxEventPublisher {

    /**
     * Publica un evento de dominio en el outbox transaccional.
     *
     * @param event el evento a publicar; no puede ser {@code null}
     * @throws IllegalArgumentException si {@code event} es {@code null}
     *                                  o si {@code event.aggregateId()} es {@code null}
     */
    void publish(DomainEvent event);
}


