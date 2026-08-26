package com.kinplatform.kin.eventbus.domain;

/**
 * Estados del ciclo de vida de un mensaje en el Outbox transaccional.
 *
 * <ul>
 *   <li>{@code PENDING}: Recién insertado, pendiente de ser procesado por el relé.</li>
 *   <li>{@code PUBLISHED}: Publicado exitosamente en el DomainEventBus.</li>
 *   <li>{@code FAILED}: Error temporal en la publicación; se reintentará según política.</li>
 *   <li>{@code DEAD_LETTER}: Agotados los reintentos; requiere intervención manual.</li>
 * </ul>
 */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED,
    DEAD_LETTER
}