package com.kinplatform.kin.health.scheduling.event;

import com.kinplatform.common.event.DomainEvent;
import com.kinplatform.common.event.HasUserId;
import java.util.UUID;

/**
 * Evento emitido cuando un médico actualiza su disponibilidad semanal.
 */
public record AvailabilityUpdatedEvent(UUID availabilityId, UUID physicianId)
        implements DomainEvent, HasUserId {

    @Override
    public String type() {
        return "availability_updated";
    }

    @Override
    public Object aggregateId() {
        return availabilityId;
    }

    @Override
    public UUID userId() {
        return physicianId;
    }
}

