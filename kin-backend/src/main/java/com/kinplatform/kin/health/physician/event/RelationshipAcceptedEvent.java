package com.kinplatform.kin.health.physician.event;

import com.kinplatform.common.event.DomainEvent;
import com.kinplatform.common.event.HasUserId;
import java.util.UUID;

/**
 * Evento emitido cuando un paciente acepta la invitación de un médico.
 *
 * <p>La relación pasa de {@code PENDING} a {@code ACTIVE}, habilitando
 * mensajería y citas. El evento alimenta notificaciones y auditoría.</p>
 */
public record RelationshipAcceptedEvent(UUID patientId, UUID physicianId)
        implements DomainEvent, HasUserId {

    @Override
    public String type() {
        return "relationship_accepted";
    }

    @Override
    public Object aggregateId() {
        return patientId;
    }

    @Override
    public UUID userId() {
        return patientId;
    }
}

