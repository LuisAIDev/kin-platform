package com.kinplatform.kin.health.differential.event;

import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.event.HasUserId;
import java.util.List;
import java.util.UUID;

/**
 * Evento de dominio emitido cuando se realiza un diagnóstico diferencial
 * (ADR-029). Implementa {@link HasUserId} para el outbox transaccional.
 */
public record DifferentialPerformedEvent(UUID userId, UUID projectId, List<String> symptoms, int itemCount)
        implements DomainEvent, HasUserId {

    @Override
    public String type() {
        return "differential_performed";
    }

    @Override
    public Object aggregateId() {
        return projectId;
    }

    @Override
    public UUID userId() {
        return userId;
    }
}
