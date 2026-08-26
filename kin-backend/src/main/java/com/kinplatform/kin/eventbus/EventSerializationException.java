package com.kinplatform.kin.eventbus;

import com.kinplatform.kin.event.DomainEvent;

/**
 * Excepción lanzada cuando falla la serialización de un evento de dominio
 * para persistencia en el Outbox transaccional.
 */
public class EventSerializationException extends RuntimeException {

    private final Object aggregateId;
    private final String eventType;

    public EventSerializationException(String message, Object aggregateId, String eventType, Throwable cause) {
        super(message, cause);
        this.aggregateId = aggregateId;
        this.eventType = eventType;
    }

    public Object aggregateId() {
        return aggregateId;
    }

    public String eventType() {
        return eventType;
    }

    public static EventSerializationException of(DomainEvent event, Throwable cause) {
        return new EventSerializationException(
                "Error serializando evento a JSON: " + event.getClass().getName(),
                event.aggregateId(),
                event.getClass().getName(),
                cause
        );
    }
}